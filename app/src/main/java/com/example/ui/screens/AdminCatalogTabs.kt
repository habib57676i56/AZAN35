package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.hardware.AdhanAudioPlayer
import com.example.core.i18n.LocalizedStrings
import com.example.data.firebase.AdhanVoiceDoc
import com.example.data.firebase.AdhkarItemDoc
import com.example.data.firebase.ReciterDoc
import com.example.data.local.AdhanVoiceEntity
import com.example.domain.AdhkarCategoryMeta
import com.example.ui.icons.MihrabIcons
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.withTabularNums
import kotlinx.coroutines.launch

@Composable
fun RecitersCrudTab(
    reciters: List<ReciterDoc>,
    isArabicUi: Boolean,
    strings: LocalizedStrings,
    onUpsert: (ReciterDoc) -> Unit,
    onDelete: (String) -> Unit,
    onSeedDefaults: () -> Unit
) {
    var editing by remember { mutableStateOf<ReciterDoc?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        editing = ReciterDoc(
                            id = "",
                            sortOrder = (reciters.maxOfOrNull { it.sortOrder } ?: 0L) + 1L,
                            active = true
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isArabicUi) "إضافة قارئ" else "Add Reciter")
                }
                OutlinedButton(onClick = onSeedDefaults, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.CloudUpload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.adminSeedDefaults)
                }
            }
        }

        items(reciters, key = { it.id.ifBlank { it.editionId } }) { r ->
            LiquidGlassCard(shape = RoundedCornerShape(22.dp), contentPadding = 14.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${r.sortOrder}. ${if (isArabicUi) r.nameAr else r.nameEn}",
                            style = MaterialTheme.typography.titleMedium.withTabularNums(),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${r.editionId} • ${r.everyAyahFolder}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onUpsert(r.copy(sortOrder = (r.sortOrder - 1L).coerceAtLeast(0L))) }) {
                            Icon(Icons.Rounded.ArrowUpward, "Move up", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onUpsert(r.copy(sortOrder = r.sortOrder + 1L)) }) {
                            Icon(Icons.Rounded.ArrowDownward, "Move down", modifier = Modifier.size(18.dp))
                        }
                        Switch(checked = r.active, onCheckedChange = { onUpsert(r.copy(active = it)) })
                        IconButton(onClick = { editing = r }) {
                            Icon(Icons.Rounded.Edit, "Edit")
                        }
                        IconButton(onClick = { onDelete(r.id) }) {
                            Icon(Icons.Rounded.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    editing?.let { current ->
        var id by remember(current) { mutableStateOf(current.id) }
        var nameAr by remember(current) { mutableStateOf(current.nameAr) }
        var nameEn by remember(current) { mutableStateOf(current.nameEn) }
        var styleAr by remember(current) { mutableStateOf(current.styleAr) }
        var styleEn by remember(current) { mutableStateOf(current.styleEn) }
        var editionId by remember(current) { mutableStateOf(current.editionId) }
        var folder by remember(current) { mutableStateOf(current.everyAyahFolder) }
        var sortOrder by remember(current) { mutableStateOf(current.sortOrder.toString()) }

        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (isArabicUi) "بيانات القارئ" else "Reciter Details") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("Doc ID (e.g. ar.alafasy)") }, singleLine = true)
                    OutlinedTextField(value = nameAr, onValueChange = { nameAr = it }, label = { Text("الاسم بالعربية") }, singleLine = true)
                    OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("English Name") }, singleLine = true)
                    OutlinedTextField(value = styleAr, onValueChange = { styleAr = it }, label = { Text("الرواية / الأسلوب") }, singleLine = true)
                    OutlinedTextField(value = styleEn, onValueChange = { styleEn = it }, label = { Text("Style (EN)") }, singleLine = true)
                    OutlinedTextField(value = editionId, onValueChange = { editionId = it }, label = { Text("AlQuranCloud Edition ID") }, singleLine = true)
                    OutlinedTextField(value = folder, onValueChange = { folder = it }, label = { Text("EveryAyah Folder") }, singleLine = true)
                    OutlinedTextField(
                        value = sortOrder,
                        onValueChange = { sortOrder = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Sort Order") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpsert(
                        current.copy(
                            id = id.ifBlank { editionId },
                            nameAr = nameAr,
                            nameEn = nameEn,
                            styleAr = styleAr,
                            styleEn = styleEn,
                            editionId = editionId,
                            everyAyahFolder = folder,
                            sortOrder = sortOrder.toLongOrNull() ?: 1L
                        )
                    )
                    editing = null
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text(strings.cancel) }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdhkarCrudTab(
    adhkarList: List<AdhkarItemDoc>,
    isArabicUi: Boolean,
    strings: LocalizedStrings,
    onUpsert: (AdhkarItemDoc) -> Unit,
    onDelete: (String) -> Unit,
    onSeedDefaults: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("ALL") }
    var editing by remember { mutableStateOf<AdhkarItemDoc?>(null) }

    val filtered = remember(adhkarList, selectedCategory) {
        if (selectedCategory == "ALL") adhkarList else adhkarList.filter { it.category == selectedCategory }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val cat = if (selectedCategory == "ALL") "MORNING" else selectedCategory
                        editing = AdhkarItemDoc(
                            id = "",
                            category = cat,
                            iconName = "WbSunny",
                            targetCount = 3L,
                            sortOrder = (adhkarList.maxOfOrNull { it.sortOrder } ?: 0L) + 1L,
                            active = true
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isArabicUi) "إضافة ذكر" else "Add Dhikr")
                }
                OutlinedButton(onClick = onSeedDefaults, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.CloudUpload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.adminSeedDefaults)
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    LiquidGlassPill(
                        selected = selectedCategory == "ALL",
                        onClick = { selectedCategory = "ALL" },
                        label = if (isArabicUi) "الكل (${adhkarList.size})" else "All (${adhkarList.size})"
                    )
                }
                items(AdhkarCategoryMeta.entries, key = { it.id }) { cat ->
                    LiquidGlassPill(
                        selected = selectedCategory == cat.id,
                        onClick = { selectedCategory = cat.id },
                        icon = MihrabIcons.iconForAdhkarCategory(cat.id),
                        label = if (isArabicUi) cat.titleAr else cat.titleEn
                    )
                }
            }
        }

        items(filtered, key = { it.id }) { item ->
            LiquidGlassCard(shape = RoundedCornerShape(22.dp), contentPadding = 14.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = MihrabIcons.iconForAdhkarItem(item.iconName, item.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isArabicUi) item.titleAr else item.titleEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${item.category} • x${item.targetCount} • ${item.iconName.ifBlank { "auto" }}",
                            style = MaterialTheme.typography.bodySmall.withTabularNums(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = item.active, onCheckedChange = { onUpsert(item.copy(active = it)) })
                    IconButton(onClick = { editing = item }) { Icon(Icons.Rounded.Edit, "Edit") }
                    IconButton(onClick = { onDelete(item.id) }) {
                        Icon(Icons.Rounded.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    editing?.let { current ->
        var id by remember(current) { mutableStateOf(current.id) }
        var category by remember(current) { mutableStateOf(current.category) }
        var titleAr by remember(current) { mutableStateOf(current.titleAr) }
        var titleEn by remember(current) { mutableStateOf(current.titleEn) }
        var arabic by remember(current) { mutableStateOf(current.arabic) }
        var translit by remember(current) { mutableStateOf(current.transliteration) }
        var translation by remember(current) { mutableStateOf(current.translation) }
        var targetCount by remember(current) { mutableStateOf(current.targetCount.toString()) }
        var refAr by remember(current) { mutableStateOf(current.referenceAr) }
        var refEn by remember(current) { mutableStateOf(current.referenceEn) }
        var iconName by remember(current) { mutableStateOf(current.iconName) }

        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (isArabicUi) "تحرير الذكر والأيقونة" else "Edit Dhikr & Icon") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("Doc ID") }, singleLine = true)
                    Text(if (isArabicUi) "القسم" else "Category", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AdhkarCategoryMeta.entries.forEach { cat ->
                            LiquidGlassPill(
                                selected = category == cat.id,
                                onClick = { category = cat.id },
                                label = if (isArabicUi) cat.titleAr else cat.titleEn
                            )
                        }
                    }
                    OutlinedTextField(value = titleAr, onValueChange = { titleAr = it }, label = { Text("العنوان بالعربية") })
                    OutlinedTextField(value = titleEn, onValueChange = { titleEn = it }, label = { Text("Title (English)") })
                    OutlinedTextField(value = arabic, onValueChange = { arabic = it }, label = { Text("النص العربي") }, minLines = 3)
                    OutlinedTextField(value = translit, onValueChange = { translit = it }, label = { Text("Transliteration") })
                    OutlinedTextField(value = translation, onValueChange = { translation = it }, label = { Text("Translation") })
                    OutlinedTextField(
                        value = targetCount,
                        onValueChange = { targetCount = it.filter { c -> c.isDigit() } },
                        label = { Text(if (isArabicUi) "عدد التكرار" else "Repeat Count") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(value = refAr, onValueChange = { refAr = it }, label = { Text("التخريج والمرجع بالعربية") })
                    OutlinedTextField(value = refEn, onValueChange = { refEn = it }, label = { Text("Reference (English)") })
                    Text(if (isArabicUi) "الأيقونة (SVG / Vector Icon)" else "Icon Name", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        MihrabIcons.selectableAdhkarIconNames.forEach { name ->
                            LiquidGlassPill(
                                selected = iconName.equals(name, ignoreCase = true),
                                onClick = { iconName = name },
                                icon = MihrabIcons.iconForAdhkarItem(name, category),
                                label = name
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpsert(
                        current.copy(
                            id = id,
                            category = category,
                            titleAr = titleAr,
                            titleEn = titleEn,
                            arabic = arabic,
                            transliteration = translit,
                            translation = translation,
                            targetCount = (targetCount.toLongOrNull() ?: 1L).coerceIn(1L, 1000L),
                            referenceAr = refAr,
                            referenceEn = refEn,
                            iconName = iconName
                        )
                    )
                    editing = null
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
fun AdhanAudioCrudTab(
    voices: List<AdhanVoiceDoc>,
    adhanAudioPlayer: AdhanAudioPlayer,
    isArabicUi: Boolean,
    strings: LocalizedStrings,
    onUpsert: (AdhanVoiceDoc) -> Unit,
    onDelete: (String) -> Unit,
    onSeedDefaults: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val playbackState by adhanAudioPlayer.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<AdhanVoiceDoc?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { editing = AdhanVoiceDoc(id = "", active = true) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isArabicUi) "إضافة أذان" else "Add Adhan Voice")
                }
                OutlinedButton(onClick = onSeedDefaults, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.CloudUpload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.adminSeedDefaults)
                }
            }
        }

        items(voices, key = { it.id }) { v ->
            val isThisPlaying = playbackState.isPlaying &&
                (playbackState.activeLabel == v.nameAr || playbackState.activeLabel == v.nameEn)
            LiquidGlassCard(shape = RoundedCornerShape(22.dp), contentPadding = 14.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isArabicUi) v.nameAr else v.nameEn, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${v.muezzin} • ${v.audioUrl}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    IconButton(onClick = {
                        if (isThisPlaying) {
                            adhanAudioPlayer.stop()
                        } else {
                            adhanAudioPlayer.playPreview(
                                voice = AdhanVoiceEntity(v.id, v.nameAr, v.nameEn, v.muezzin, v.audioUrl, v.active),
                                mode = com.example.data.firebase.AdhanAlertMode.FULL_ADHAN
                            )
                        }
                    }) {
                        Icon(if (isThisPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, contentDescription = "Preview")
                    }
                    Switch(checked = v.active, onCheckedChange = { onUpsert(v.copy(active = it)) })
                    IconButton(onClick = { editing = v }) { Icon(Icons.Rounded.Edit, "Edit") }
                    IconButton(onClick = { onDelete(v.id) }) {
                        Icon(Icons.Rounded.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    editing?.let { current ->
        var id by remember(current) { mutableStateOf(current.id) }
        var nameAr by remember(current) { mutableStateOf(current.nameAr) }
        var nameEn by remember(current) { mutableStateOf(current.nameEn) }
        var muezzin by remember(current) { mutableStateOf(current.muezzin) }
        var audioUrl by remember(current) { mutableStateOf(current.audioUrl) }
        var pickedLocalFile by remember { mutableStateOf<String?>(null) }

        val pickAudioLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    val copied = adhanAudioPlayer.importCustomAudioFromUri(uri).getOrNull()
                    if (copied != null) {
                        pickedLocalFile = copied.first
                        if (nameEn.isBlank()) nameEn = copied.second.substringBeforeLast('.')
                        adhanAudioPlayer.playPreview(
                            voice = AdhanVoiceEntity(
                                id = "custom_preview",
                                nameAr = copied.second,
                                nameEn = copied.second,
                                muezzin = muezzin,
                                audioUrl = "",
                                active = true
                            ),
                            mode = com.example.data.firebase.AdhanAlertMode.FULL_ADHAN,
                            customLocalPath = copied.first,
                            customDisplayName = copied.second
                        )
                    }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (isArabicUi) "إدارة تسجيل الأذان" else "Manage Adhan Recording") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("Voice ID (e.g. makkah)") }, singleLine = true)
                    OutlinedTextField(value = nameAr, onValueChange = { nameAr = it }, label = { Text("الاسم بالعربية") }, singleLine = true)
                    OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("English Name") }, singleLine = true)
                    OutlinedTextField(value = muezzin, onValueChange = { muezzin = it }, label = { Text(if (isArabicUi) "اسم المؤذن" else "Muezzin") }, singleLine = true)
                    OutlinedTextField(value = audioUrl, onValueChange = { audioUrl = it }, label = { Text("Public Audio URL (HTTPS MP3)") }, singleLine = true)
                    OutlinedButton(
                        onClick = { pickAudioLauncher.launch(arrayOf("audio/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.UploadFile, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isArabicUi) "اختبار ملف صوتي من الجهاز" else "Test Local Audio File")
                    }
                    if (pickedLocalFile != null) {
                        Text(
                            text = if (isArabicUi) "تم فحص الملف محلياً. أدخل رابط HTTPS أعلاه لإتاحته لجميع المستخدمين." else "Local audio verified. Provide an HTTPS URL above so all users can stream it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpsert(
                            current.copy(
                                id = id,
                                nameAr = nameAr,
                                nameEn = nameEn,
                                muezzin = muezzin,
                                audioUrl = audioUrl
                            )
                        )
                        editing = null
                    },
                    enabled = audioUrl.isNotBlank()
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text(strings.cancel) }
            }
        )
    }
}
