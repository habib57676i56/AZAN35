package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.hardware.QuranRecitationPlayer
import com.example.core.hardware.QuranTtsAudioPlayer
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalizedStrings
import com.example.data.firebase.AppOperationsConfig
import com.example.data.firebase.UserSettingsDoc
import com.example.domain.ArabicTextNormalizer
import com.example.domain.QuranCatalog
import com.example.ui.icons.MihrabIcons
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.withTabularNums
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.MihrabViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun QuranScreen(
    viewModel: MihrabViewModel,
    aiViewModel: AiViewModel,
    audioPlayer: QuranTtsAudioPlayer,
    recitationPlayer: QuranRecitationPlayer,
    appLanguage: AppLanguage,
    strings: LocalizedStrings,
    appOperations: AppOperationsConfig = AppOperationsConfig(),
    onOpenAiAssistant: () -> Unit = {}
) {
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val bookmarksState by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val groupsState by viewModel.khatmahGroupsState.collectAsStateWithLifecycle()

    val settings = (settingsState as? UiState.Success)?.data ?: UserSettingsDoc()
    val isArabicUi = appLanguage == AppLanguage.ARABIC || appLanguage == AppLanguage.URDU

    var activeSection by remember { mutableIntStateOf(0) } // 0=Madinah Mushaf, 1=Al-Musammi', 2=Khatmah, 3=Bookmarks
    var externalTargetPage by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val tabs = listOf(
            strings.mushafReader to Icons.Rounded.AutoStories,
            strings.alMusammi to Icons.Rounded.Mic,
            strings.khatmahPlan to Icons.Rounded.CheckCircle,
            strings.bookmarksLabel to MihrabIcons.Bookmark
        )
        LazyRow(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs.size) { idx ->
                val (label, icon) = tabs[idx]
                LiquidGlassPill(
                    selected = activeSection == idx,
                    onClick = { activeSection = idx },
                    icon = icon,
                    label = label,
                    modifier = Modifier.testTag("quran_tab_$idx")
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            when (activeSection) {
                0 -> {
                    MadinahMushafReader(
                        viewModel = viewModel,
                        aiViewModel = aiViewModel,
                        recitationPlayer = recitationPlayer,
                        settings = settings,
                        appLanguage = appLanguage,
                        strings = strings,
                        externalTargetPage = externalTargetPage,
                        onExternalPageConsumed = { externalTargetPage = null },
                        quranAudioEnabled = appOperations.quranAudioEnabled,
                        aiAssistantEnabled = appOperations.aiAssistantEnabled,
                        onOpenAiAssistant = onOpenAiAssistant
                    )
                }

                1 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 640.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            AlMusammiSection(aiViewModel = aiViewModel)
                        }
                    }
                }

                2 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 640.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            KhatmahAndGroupsSection(
                                settings = settings,
                                groupsState = groupsState,
                                viewModel = viewModel,
                                familyKhatmahEnabled = appOperations.familyKhatmahEnabled
                            )
                        }
                    }
                }

                3 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 640.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (val bm = bookmarksState) {
                            is UiState.Loading -> item {
                                Text(if (isArabicUi) "جارٍ تحميل العلامات..." else "Loading bookmarks...")
                            }
                            is UiState.Error -> item {
                                Text(bm.message, color = MaterialTheme.colorScheme.error)
                            }
                            is UiState.Success -> {
                                if (bm.data.isEmpty()) {
                                    item {
                                        LiquidGlassCard(
                                            shape = RoundedCornerShape(24.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Bookmark,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = if (isArabicUi) {
                                                        "لا توجد علامات مرجعية بعد. اضغط على أي آية في مصحف المدينة لحفظ علامة أو ملاحظة والعودة إليها سريعاً."
                                                    } else {
                                                        "No verse bookmarks yet. Tap any Ayah in the Madinah Mushaf to save bookmarks or notes."
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    items(bm.data, key = { it.id }) { item ->
                                        val surahMeta = QuranCatalog.surahByNumber(item.surahNumber.toInt())
                                        val targetPage = item.pageNumber?.toInt()?.coerceIn(1, 604) ?: surahMeta.startPage
                                        LiquidGlassCard(
                                            onClick = {
                                                externalTargetPage = targetPage
                                                activeSection = 0
                                            },
                                            contentPadding = 14.dp,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (isArabicUi) {
                                                            "سورة ${surahMeta.nameAr} • آية ${ArabicTextNormalizer.toArabicIndicDigits(item.ayahNumber.toInt())} (${strings.pageLabel} ${ArabicTextNormalizer.toArabicIndicDigits(targetPage)})"
                                                        } else {
                                                            "Surah ${surahMeta.nameEn} (${item.surahNumber}:${item.ayahNumber}) • ${strings.pageLabel} $targetPage"
                                                        },
                                                        style = MaterialTheme.typography.titleMedium.withTabularNums(),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        text = item.collectionName,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    if (!item.note.isNullOrBlank()) {
                                                        Spacer(Modifier.height(4.dp))
                                                        Text(item.note, style = MaterialTheme.typography.bodyMedium)
                                                    }
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(onClick = {
                                                        externalTargetPage = targetPage
                                                        activeSection = 0
                                                    }) {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                                            contentDescription = strings.mushafReader,
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    IconButton(onClick = { viewModel.deleteBookmark(item.id) }) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Delete,
                                                            contentDescription = if (isArabicUi) "حذف العلامة" else "Delete bookmark"
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
    }
}
