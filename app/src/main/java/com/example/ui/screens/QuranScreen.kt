package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.hardware.QuranTtsAudioPlayer
import com.example.core.hardware.shareDesignedVerseTextOrCard
import com.example.core.i18n.AppLanguage
import com.example.data.firebase.UserSettingsDoc
import com.example.domain.QuranAyah
import com.example.domain.QuranContentData
import com.example.domain.QuranSurah
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.BurnishedGold
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.MihrabViewModel
import com.example.ui.viewmodel.UiState

private enum class MushafPaperMode(val label: String, val bg: Color, val fg: Color) {
    PAPER("Paper", Color(0xFFFCF7EB), Color(0xFF1A241E)),
    NIGHT_WARM("Night Warm", Color(0xFF231D16), Color(0xFFEEDCC3)),
    DARK("Dark", Color(0xFF0E1714), Color(0xFFE5EFEA))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuranScreen(
    viewModel: MihrabViewModel,
    aiViewModel: AiViewModel,
    audioPlayer: QuranTtsAudioPlayer,
    appLanguage: AppLanguage
) {
    val context = LocalContext.current
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val bookmarksState by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val groupsState by viewModel.khatmahGroupsState.collectAsStateWithLifecycle()
    val audioState by audioPlayer.state.collectAsStateWithLifecycle()

    val settings = (settingsState as? UiState.Success)?.data ?: UserSettingsDoc()

    var activeSection by remember { mutableIntStateOf(0) } // 0=Mushaf, 1=Al-Musammi, 2=Khatmah, 3=Bookmarks
    var searchQuery by remember { mutableStateOf("") }
    var selectedThemeTag by remember { mutableStateOf("All") }
    var paperMode by remember { mutableStateOf(MushafPaperMode.PAPER) }
    var focusBarlessMode by remember { mutableStateOf(false) }
    var arabicFontSize by remember { mutableFloatStateOf(26f) }
    var tafsirSource by remember { mutableIntStateOf(0) } // 0=Muyassar, 1=IbnKathir, 2=Al-Sa'di
    var repeatCount by remember { mutableIntStateOf(1) }
    var audioInEnglishAfter by remember { mutableStateOf(false) }

    var noteTarget by remember { mutableStateOf<Pair<QuranSurah, QuranAyah>?>(null) }
    var noteText by remember { mutableStateOf("") }
    var collectionText by remember { mutableStateOf("Favorites") }

    val filteredAyahs = remember(searchQuery, selectedThemeTag) {
        QuranContentData.searchAyahs(searchQuery, selectedThemeTag)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!focusBarlessMode) {
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Mushaf", "Al-Musammi'", "Khatmah", "Bookmarks").forEachIndexed { idx, label ->
                            FilterChip(
                                selected = activeSection == idx,
                                onClick = { activeSection = idx },
                                label = { Text(label) },
                                modifier = Modifier.testTag("quran_tab_$idx")
                            )
                        }
                    }
                }
            }

            when (activeSection) {
                0 -> {
                    if (!focusBarlessMode) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                                        label = { Text("Search Ayah, Surah, or keyword") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        QuranContentData.thematicTopics.forEach { topic ->
                                            FilterChip(
                                                selected = selectedThemeTag == topic,
                                                onClick = { selectedThemeTag = topic },
                                                label = { Text(topic) }
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            MushafPaperMode.entries.forEach { mode ->
                                                FilterChip(
                                                    selected = paperMode == mode,
                                                    onClick = { paperMode = mode },
                                                    label = { Text(mode.label) }
                                                )
                                            }
                                        }
                                        IconButton(onClick = { focusBarlessMode = true }) {
                                            Icon(Icons.Rounded.VisibilityOff, contentDescription = "Distraction-Free Focus Mode")
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Arabic Size", style = MaterialTheme.typography.labelLarge)
                                        Spacer(Modifier.width(8.dp))
                                        Slider(
                                            value = arabicFontSize,
                                            onValueChange = { arabicFontSize = it },
                                            valueRange = 20f..36f,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // Audio & Sleep Timer bar
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf(1, 3, 5).forEach { r ->
                                                FilterChip(
                                                    selected = repeatCount == r,
                                                    onClick = { repeatCount = r },
                                                    label = { Text("${r}x") }
                                                )
                                            }
                                            FilterChip(
                                                selected = audioInEnglishAfter,
                                                onClick = { audioInEnglishAfter = !audioInEnglishAfter },
                                                label = { Text("Audio Meaning") }
                                            )
                                        }
                                        if (audioState.isPlaying) {
                                            OutlinedButton(onClick = { audioPlayer.stop() }) {
                                                Icon(Icons.Rounded.Stop, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Stop")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            OutlinedButton(onClick = { focusBarlessMode = false }) {
                                Text("Exit Distraction-Free Focus Mode")
                            }
                        }
                    }

                    items(filteredAyahs, key = { "${it.first.number}_${it.second.number}" }) { (surah, ayah) ->
                        AyahCardItem(
                            surah = surah,
                            ayah = ayah,
                            paperMode = paperMode,
                            arabicFontSize = arabicFontSize,
                            tafsirSource = tafsirSource,
                            appLanguage = appLanguage,
                            focusMode = focusBarlessMode,
                            onChangeTafsirSource = { tafsirSource = it },
                            onPlay = {
                                val textToSpeak = if (audioInEnglishAfter) {
                                    "${ayah.arabic}. ${ayah.translationEn}"
                                } else {
                                    ayah.arabic
                                }
                                audioPlayer.speak(
                                    title = "${surah.nameEn} ${surah.number}:${ayah.number}",
                                    text = textToSpeak,
                                    preferArabic = !audioInEnglishAfter,
                                    ayahIndex = ayah.number,
                                    repeatCount = repeatCount
                                )
                            },
                            onBookmark = {
                                noteTarget = surah to ayah
                                noteText = ""
                            },
                            onShare = {
                                shareDesignedVerseTextOrCard(
                                    context = context,
                                    surahLabel = "${surah.nameEn} (${surah.number}:${ayah.number})",
                                    arabicText = ayah.arabic,
                                    translation = ayah.translationEn
                                )
                            }
                        )
                    }
                }
                1 -> item { AlMusammiSection(aiViewModel = aiViewModel) }
                2 -> item {
                    KhatmahAndGroupsSection(
                        settings = settings,
                        groupsState = groupsState,
                        viewModel = viewModel
                    )
                }
                3 -> {
                    when (val bm = bookmarksState) {
                        is UiState.Loading -> item { Text("Loading bookmarks...") }
                        is UiState.Error -> item { Text(bm.message, color = MaterialTheme.colorScheme.error) }
                        is UiState.Success -> {
                            if (bm.data.isEmpty()) {
                                item {
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            "No verse bookmarks yet. Tap Bookmark on any Ayah in the Mushaf to save verses into custom collections (e.g. Patience, Protection).",
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            } else {
                                items(bm.data, key = { it.id }) { item ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    "Surah ${item.surahNumber} : Ayah ${item.ayahNumber} • ${item.collectionName}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (!item.note.isNullOrBlank()) {
                                                    Text(item.note, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                            IconButton(onClick = { viewModel.deleteBookmark(item.id) }) {
                                                Icon(Icons.Rounded.Delete, contentDescription = "Delete bookmark")
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

    noteTarget?.let { (surah, ayah) ->
        AlertDialog(
            onDismissRequest = { noteTarget = null },
            title = { Text("Save ${surah.nameEn} ${surah.number}:${ayah.number}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = collectionText,
                        onValueChange = { collectionText = it },
                        label = { Text("Collection (e.g. Favorites, Patience, Protection)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Personal reflection note (optional)") },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addBookmark(surah.number, ayah.number, collectionText, noteText)
                    if (noteText.isNotBlank()) {
                        viewModel.addJournalEntry(
                            entryType = "VERSE_NOTE",
                            title = "${surah.nameEn} (${surah.number}:${ayah.number})",
                            body = noteText,
                            reference = "Surah ${surah.number}:${ayah.number}"
                        )
                    }
                    noteTarget = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AyahCardItem(
    surah: QuranSurah,
    ayah: QuranAyah,
    paperMode: MushafPaperMode,
    arabicFontSize: Float,
    tafsirSource: Int,
    appLanguage: AppLanguage,
    focusMode: Boolean,
    onChangeTafsirSource: (Int) -> Unit,
    onPlay: () -> Unit,
    onBookmark: () -> Unit,
    onShare: () -> Unit
) {
    var showInsights by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = paperMode.bg),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${surah.nameEn} • ${surah.number}:${ayah.number} (Juz ${surah.juzNumber})",
                    style = MaterialTheme.typography.labelLarge,
                    color = BurnishedGold,
                    fontWeight = FontWeight.Bold
                )
                if (ayah.sajdahRequired) {
                    Surface(color = BurnishedGold.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small) {
                        Text(
                            "۩ Sajdah Tilawah",
                            style = MaterialTheme.typography.labelSmall,
                            color = BurnishedGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Text(
                text = ayah.arabic,
                fontFamily = AmiriFontFamily,
                fontSize = arabicFontSize.sp,
                lineHeight = (arabicFontSize * 1.65f).sp,
                color = paperMode.fg,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            if (!focusMode) {
                Text(
                    text = ayah.transliteration,
                    style = MaterialTheme.typography.bodyMedium,
                    color = paperMode.fg.copy(alpha = 0.72f)
                )
                val translation = when (appLanguage) {
                    AppLanguage.TURKISH -> ayah.translationTr
                    AppLanguage.URDU -> ayah.translationUr
                    else -> ayah.translationEn
                }
                Text(
                    text = translation,
                    style = MaterialTheme.typography.bodyLarge,
                    color = paperMode.fg
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        IconButton(onClick = onPlay) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Listen to Ayah", tint = BurnishedGold)
                        }
                        IconButton(onClick = onBookmark) {
                            Icon(Icons.Rounded.Bookmark, contentDescription = "Bookmark Ayah", tint = paperMode.fg)
                        }
                        IconButton(onClick = onShare) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share Verse", tint = paperMode.fg)
                        }
                    }
                    OutlinedButton(onClick = { showInsights = !showInsights }) {
                        Icon(Icons.Rounded.EditNote, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (showInsights) "Hide Tafsir" else "Tafsir & Tajweed")
                    }
                }

                if (showInsights) {
                    HorizontalDivider(color = paperMode.fg.copy(alpha = 0.15f))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Al-Muyassar", "Ibn Kathir", "Al-Sa'di").forEachIndexed { idx, name ->
                            FilterChip(
                                selected = tafsirSource == idx,
                                onClick = { onChangeTafsirSource(idx) },
                                label = { Text(name) }
                            )
                        }
                    }
                    val tafsirText = when (tafsirSource) {
                        1 -> ayah.tafsirIbnKathir
                        2 -> ayah.tafsirSadi
                        else -> ayah.tafsirMuyassar
                    }
                    Text(tafsirText, style = MaterialTheme.typography.bodyMedium, color = paperMode.fg)

                    if (ayah.wordByWord.isNotEmpty()) {
                        Text("Word-by-Word", style = MaterialTheme.typography.labelLarge, color = BurnishedGold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ayah.wordByWord.forEach { w ->
                                Surface(color = paperMode.fg.copy(alpha = 0.08f), shape = MaterialTheme.shapes.small) {
                                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(w.arabic, fontFamily = AmiriFontFamily, fontSize = 18.sp, color = paperMode.fg)
                                        Text(w.meaning, style = MaterialTheme.typography.labelSmall, color = paperMode.fg.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }

                    Text("Tajweed & Waqf: ${ayah.tajweedWaqfNote}", style = MaterialTheme.typography.bodyMedium, color = paperMode.fg.copy(alpha = 0.85f))
                    Text("Asbab al-Nuzul: ${ayah.asbabAlNuzul}", style = MaterialTheme.typography.bodyMedium, color = paperMode.fg.copy(alpha = 0.85f))
                    Text("Qira'at (Hafs / Warsh): ${ayah.qiraatNote}", style = MaterialTheme.typography.bodyMedium, color = paperMode.fg.copy(alpha = 0.85f))
                    Text("Rare Word (Tadabbur): ${ayah.rareWordMeaning}", style = MaterialTheme.typography.bodyMedium, color = paperMode.fg.copy(alpha = 0.85f))
                    Text("Similar Verses (Mutashabihat): ${ayah.mutashabihatRef}", style = MaterialTheme.typography.bodyMedium, color = paperMode.fg.copy(alpha = 0.85f))
                }
            }
        }
    }
}
