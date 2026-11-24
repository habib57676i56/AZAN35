package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.hardware.QuranRecitationPlayer
import com.example.core.hardware.RecitationPlayMode
import com.example.core.hardware.RecitationPlaybackState
import com.example.core.hardware.shareDesignedVerseTextOrCard
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalizedStrings
import com.example.data.firebase.UserSettingsDoc
import com.example.data.local.ReciterEntity
import com.example.domain.ArabicTextNormalizer
import com.example.domain.MushafAyahItem
import com.example.domain.MushafPageModel
import com.example.domain.QuranCatalog
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.MushafPaperTheme
import com.example.ui.viewmodel.AiUiState
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.MihrabViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MadinahMushafReader(
    viewModel: MihrabViewModel,
    aiViewModel: AiViewModel,
    recitationPlayer: QuranRecitationPlayer,
    settings: UserSettingsDoc,
    appLanguage: AppLanguage,
    strings: LocalizedStrings,
    externalTargetPage: Int?,
    onExternalPageConsumed: () -> Unit,
    quranAudioEnabled: Boolean = true,
    aiAssistantEnabled: Boolean = true,
    onOpenAiAssistant: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isArabicUi = appLanguage == AppLanguage.ARABIC || appLanguage == AppLanguage.URDU

    val pagesMap by viewModel.mushafPages.collectAsStateWithLifecycle()
    val reciters by viewModel.recitersState.collectAsStateWithLifecycle()
    val cachedCount by viewModel.cachedMushafPageCount.collectAsStateWithLifecycle()
    val offlineProgress by viewModel.offlineSyncProgress.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val audioState by recitationPlayer.state.collectAsStateWithLifecycle()
    val ayahTafsirState by aiViewModel.ayahTafsirState.collectAsStateWithLifecycle()

    val initialPageIndex = (settings.lastReadPage.toInt().coerceIn(1, 604) - 1)
    val pagerState = rememberPagerState(initialPage = initialPageIndex, pageCount = { 604 })

    var focusBarlessMode by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showNavigatorSheet by remember { mutableStateOf(false) }
    var showReaderSettingsSheet by remember { mutableStateOf(false) }
    var showReciterSheet by remember { mutableStateOf(false) }
    var showLoopDialog by remember { mutableStateOf(false) }

    var selectedAyahForAction by remember { mutableStateOf<MushafAyahItem?>(null) }
    var noteDialogAyah by remember { mutableStateOf<MushafAyahItem?>(null) }
    var noteInput by remember { mutableStateOf("") }
    var collectionInput by remember { mutableStateOf("المفضلة") }
    var showAiTafsirPanel by remember { mutableStateOf(false) }

    val paperTheme = MushafPaperTheme.fromId(settings.mushafPaper)
    val fontScale = settings.fontScale.toFloat().coerceIn(0.8f, 1.5f)
    val activePageNum = pagerState.currentPage + 1

    // Sync selected reciter from user preferences once loaded
    LaunchedEffect(settings.preferredReciterId, reciters) {
        val preferred = reciters.firstOrNull { it.id == settings.preferredReciterId }
        if (preferred != null && audioState.reciter.id != preferred.id && !audioState.isPlaying) {
            recitationPlayer.setReciter(preferred)
        }
    }

    // Jump to bookmark or external page target
    LaunchedEffect(externalTargetPage) {
        if (externalTargetPage != null) {
            val targetIdx = (externalTargetPage - 1).coerceIn(0, 603)
            pagerState.animateScrollToPage(targetIdx)
            onExternalPageConsumed()
        }
    }

    // Load visible page & save last read position to Room + Firestore
    LaunchedEffect(activePageNum) {
        viewModel.ensureMushafPageLoaded(activePageNum)
        val pageModel = (pagesMap[activePageNum] as? UiState.Success)?.data
        val firstAyah = pageModel?.ayahs?.firstOrNull()
        val surah = firstAyah?.surahNumber ?: QuranCatalog.surahs.lastOrNull { it.startPage <= activePageNum }?.number ?: 1
        val ayah = firstAyah?.numberInSurah ?: 1
        viewModel.saveLastReadPosition(activePageNum, surah, ayah)
    }

    // Auto page-turning synced with audio recitation playback
    LaunchedEffect(audioState.isPlaying, audioState.currentPage) {
        if (audioState.isPlaying && audioState.currentPage in 1..604) {
            val targetIdx = audioState.currentPage - 1
            if (pagerState.currentPage != targetIdx) {
                pagerState.animateScrollToPage(targetIdx)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Madinah Mushaf Toolbar (hidden in Focus mode)
        if (!focusBarlessMode) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showNavigatorSheet = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("mushaf_navigator_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.List, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${strings.pageLabel} ${formatPageNum(activePageNum, isArabicUi)}",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (cachedCount >= 604) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.secondaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.clickable { showReaderSettingsSheet = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (cachedCount >= 604) Icons.Rounded.CloudDone else Icons.Rounded.CloudDownload,
                                        contentDescription = strings.offlineCachedBadge,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "${formatPageNum(cachedCount, isArabicUi)}/${formatPageNum(604, isArabicUi)}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showSearchSheet = true },
                                modifier = Modifier.testTag("mushaf_search_button")
                            ) {
                                Icon(Icons.Rounded.Search, contentDescription = strings.searchQuranHint)
                            }
                            IconButton(
                                onClick = { showReaderSettingsSheet = true },
                                modifier = Modifier.testTag("mushaf_settings_button")
                            ) {
                                Icon(Icons.Rounded.Tune, contentDescription = strings.fontScaleLabel)
                            }
                            IconButton(onClick = { focusBarlessMode = true }) {
                                Icon(Icons.Rounded.VisibilityOff, contentDescription = strings.focusModeLabel)
                            }
                        }
                    }

                    if (offlineProgress != null) {
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { offlineProgress!! },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { focusBarlessMode = false }) {
                    Icon(Icons.Rounded.Visibility, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.exitFocusMode)
                }
            }
        }

        // Authentic RTL HorizontalPager across all 604 Madinah Mushaf pages
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize()
                ) { pageIdx ->
                    val pageNum = pageIdx + 1
                    LaunchedEffect(pageNum) {
                        viewModel.ensureMushafPageLoaded(pageNum)
                    }
                    val pageState = pagesMap[pageNum] ?: UiState.Loading

                    MushafPageFrame(
                        pageNumber = pageNum,
                        pageState = pageState,
                        paperTheme = paperTheme,
                        fontScale = fontScale,
                        isArabicUi = isArabicUi,
                        strings = strings,
                        activePlayingGlobalAyah = if (audioState.isPlaying || audioState.isPaused) audioState.currentGlobalAyah else -1,
                        selectedGlobalAyah = selectedAyahForAction?.globalNumber ?: -1,
                        onRetry = { viewModel.retryMushafPage(pageNum) },
                        onAyahTapped = { ayah ->
                            selectedAyahForAction = ayah
                            showAiTafsirPanel = false
                            aiViewModel.resetAyahTafsir()
                        },
                        onPrevPage = {
                            if (pageIdx > 0) scope.launch { pagerState.animateScrollToPage(pageIdx - 1) }
                        },
                        onNextPage = {
                            if (pageIdx < 603) scope.launch { pagerState.animateScrollToPage(pageIdx + 1) }
                        }
                    )
                }
            }
        }

        LaunchedEffect(quranAudioEnabled) {
            if (!quranAudioEnabled) recitationPlayer.stop()
        }

        // Docked Multi-Reciter Synced Audio Bar (hidden in Focus mode unless playing, or when disabled by Admin)
        if (quranAudioEnabled && (!focusBarlessMode || audioState.isPlaying || audioState.isBuffering)) {
            val activeModel = (pagesMap[activePageNum] as? UiState.Success)?.data
            DockedRecitationAudioBar(
                audioState = audioState,
                isArabicUi = isArabicUi,
                strings = strings,
                onTogglePlay = {
                    if (audioState.isPlaying || audioState.isPaused) {
                        recitationPlayer.togglePlayPause()
                    } else {
                        val firstAyah = activeModel?.ayahs?.firstOrNull()
                        val surah = firstAyah?.surahNumber ?: 1
                        val ayah = firstAyah?.numberInSurah ?: 1
                        recitationPlayer.playAyah(surah, ayah, activePageNum)
                    }
                },
                onStop = { recitationPlayer.stop() },
                onNext = { recitationPlayer.nextAyah() },
                onPrev = { recitationPlayer.previousAyah() },
                onCycleSpeed = {
                    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f)
                    val nextIdx = (speeds.indexOfFirst { kotlin.math.abs(it - audioState.speed) < 0.05f } + 1) % speeds.size
                    recitationPlayer.setSpeed(speeds[nextIdx])
                },
                onOpenReciters = { showReciterSheet = true },
                onOpenLoopDialog = { showLoopDialog = true },
                onCachePageAudio = {
                    activeModel?.let { recitationPlayer.cachePageAyahsOffline(it.ayahs) }
                }
            )
        }
    }

    // --- AYAH TAP ACTION SHEET ---
    selectedAyahForAction?.let { ayah ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedAyahForAction = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabicUi) {
                            "سورة ${ayah.surahNameAr} • الآية ${ArabicTextNormalizer.toArabicIndicDigits(ayah.numberInSurah)}"
                        } else {
                            "Surah ${ayah.surahNameEn} • ${ayah.surahNumber}:${ayah.numberInSurah}"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${strings.pageLabel} ${formatPageNum(ayah.pageNumber, isArabicUi)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = BurnishedGold
                    )
                }

                Surface(
                    color = paperTheme.cardInner,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${ayah.textUthmani} ${ArabicTextNormalizer.formatVerseMedallion(ayah.numberInSurah)}",
                        fontFamily = AmiriQuranFontFamily,
                        fontSize = 23.sp,
                        lineHeight = 42.sp,
                        color = paperTheme.fg,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (quranAudioEnabled) {
                        Button(
                            onClick = {
                                recitationPlayer.playAyah(
                                    surahNumber = ayah.surahNumber,
                                    ayahNumber = ayah.numberInSurah,
                                    pageNumber = ayah.pageNumber
                                )
                                selectedAyahForAction = null
                            },
                            modifier = Modifier.testTag("ayah_action_play")
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.playFromAyah)
                        }
                    }

                    if (aiAssistantEnabled) {
                        OutlinedButton(
                            onClick = {
                                showAiTafsirPanel = true
                                aiViewModel.askAiTafsirForAyah(
                                    surahNameAr = ayah.surahNameAr,
                                    surahNumber = ayah.surahNumber,
                                    ayahNumber = ayah.numberInSurah,
                                    arabicText = ayah.textUthmani,
                                    languageName = appLanguage.nativeLabel
                                )
                            },
                            modifier = Modifier.testTag("ayah_action_ai_tafsir")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.askAiTafsir)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val clipMgr = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipText = "${ayah.textUthmani} (${ayah.surahNameAr} ${ayah.surahNumber}:${ayah.numberInSurah})"
                            clipMgr.setPrimaryClip(ClipData.newPlainText("Quran Ayah", clipText))
                            viewModel.showMessage(strings.copiedToast)
                            selectedAyahForAction = null
                        }
                    ) {
                        Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.copyAyah)
                    }

                    OutlinedButton(
                        onClick = {
                            shareDesignedVerseTextOrCard(
                                context = context,
                                surahLabel = "${ayah.surahNameAr} (${ayah.surahNumber}:${ayah.numberInSurah})",
                                arabicText = ayah.textUthmani,
                                translation = "${ayah.surahNameEn} • Page ${ayah.pageNumber}"
                            )
                            selectedAyahForAction = null
                        }
                    ) {
                        Icon(Icons.Rounded.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.shareAyah)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.addBookmark(
                                surahNumber = ayah.surahNumber,
                                ayahNumber = ayah.numberInSurah,
                                collectionName = if (isArabicUi) "المفضلة" else "Favorites",
                                note = null,
                                pageNumber = ayah.pageNumber
                            )
                            selectedAyahForAction = null
                        }
                    ) {
                        Icon(Icons.Rounded.Bookmark, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.bookmarkAyah)
                    }

                    OutlinedButton(
                        onClick = {
                            noteTargetDialog(
                                ayah = ayah,
                                isArabicUi = isArabicUi,
                                onOpen = { a, col ->
                                    selectedAyahForAction = null
                                    collectionInput = col
                                    noteInput = ""
                                    noteDialogAyah = a
                                }
                            )
                        }
                    ) {
                        Icon(Icons.Rounded.EditNote, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.addNoteAyah)
                    }
                }

                if (showAiTafsirPanel) {
                    HorizontalDivider()
                    when (val ts = ayahTafsirState) {
                        is AiUiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(if (isArabicUi) "جارٍ استحضار التفسير ومعاني المفردات..." else "Fetching AI Tafsir & word meanings...")
                        }
                        is AiUiState.Streaming -> Text(ts.text, style = MaterialTheme.typography.bodyLarge)
                        is AiUiState.Success -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(ts.text, style = MaterialTheme.typography.bodyLarge)
                            OutlinedButton(
                                onClick = {
                                    selectedAyahForAction = null
                                    onOpenAiAssistant()
                                },
                                modifier = Modifier.fillMaxWidth().testTag("open_full_ai_from_tafsir")
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (isArabicUi) "متابعة الحوار في المساعد الذكي (محفوظ في السجل)"
                                    else "Continue in AI Assistant (Saved to History)"
                                )
                            }
                        }
                        is AiUiState.Error -> Text(ts.message, color = MaterialTheme.colorScheme.error)
                        else -> Unit
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // --- NOTE & BOOKMARK DIALOG ---
    noteDialogAyah?.let { ayah ->
        AlertDialog(
            onDismissRequest = { noteDialogAyah = null },
            title = {
                Text("${ayah.surahNameAr} (${ayah.surahNumber}:${ayah.numberInSurah})")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = collectionInput,
                        onValueChange = { collectionInput = it },
                        label = { Text(if (isArabicUi) "المجموعة" else "Collection") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text(if (isArabicUi) "تأملاتك وملاحظاتك الشخصية" else "Personal reflection note") },
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addBookmark(
                        surahNumber = ayah.surahNumber,
                        ayahNumber = ayah.numberInSurah,
                        collectionName = collectionInput,
                        note = noteInput,
                        pageNumber = ayah.pageNumber
                    )
                    if (noteInput.isNotBlank()) {
                        viewModel.addJournalEntry(
                            entryType = "VERSE_NOTE",
                            title = "${ayah.surahNameAr} (${ayah.surahNumber}:${ayah.numberInSurah})",
                            body = noteInput,
                            reference = "ص ${ayah.pageNumber}"
                        )
                    }
                    noteDialogAyah = null
                }) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteDialogAyah = null }) { Text(strings.cancel) }
            }
        )
    }

    // --- SMART FULL-QURAN ARABIC SEARCH SHEET ---
    if (showSearchSheet) {
        var searchText by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showSearchSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 380.dp, max = 620.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        viewModel.searchQuran(it)
                    },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = {
                                searchText = ""
                                viewModel.searchQuran("")
                            }) { Icon(Icons.Rounded.Close, null) }
                        }
                    },
                    label = { Text(strings.searchQuranHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("smart_quran_search_input")
                )

                if (isSearching) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (searchResults.isEmpty() && searchText.trim().length >= 2 && !isSearching) {
                    Text(
                        text = if (isArabicUi) "لا توجد نتائج مطابقة. جرّب كلمة قرآنية أخرى أو قم بتنزيل المصحف كاملاً للبحث الفوري."
                        else "No matching verses found. Try another Arabic root or sync the full Mushaf offline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(searchResults, key = { it.globalAyahNumber }) { hit ->
                        Card(
                            onClick = {
                                showSearchSheet = false
                                scope.launch {
                                    pagerState.animateScrollToPage((hit.pageNumber - 1).coerceIn(0, 603))
                                }
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = "${hit.surahNameAr} (${hit.surahNumber}:${hit.ayahNumber})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${strings.pageLabel} ${formatPageNum(hit.pageNumber, isArabicUi)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = BurnishedGold
                                    )
                                }
                                Text(
                                    text = hit.uthmaniText,
                                    fontFamily = AmiriQuranFontFamily,
                                    fontSize = 20.sp,
                                    lineHeight = 34.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- 114 SURAH / 30 JUZ / PAGE NAVIGATOR SHEET ---
    if (showNavigatorSheet) {
        var navTab by remember { mutableIntStateOf(0) } // 0=Surahs, 1=Juz, 2=Page Jump
        var pageJumpText by remember { mutableStateOf(activePageNum.toString()) }
        ModalBottomSheet(onDismissRequest = { showNavigatorSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 420.dp, max = 620.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = navTab == 0, onClick = { navTab = 0 }, label = { Text(strings.surahIndex) })
                    FilterChip(selected = navTab == 1, onClick = { navTab = 1 }, label = { Text(strings.juzIndex) })
                    FilterChip(selected = navTab == 2, onClick = { navTab = 2 }, label = { Text(strings.pageLabel) })
                }

                when (navTab) {
                    0 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(QuranCatalog.surahs, key = { it.number }) { s ->
                            Card(
                                onClick = {
                                    showNavigatorSheet = false
                                    scope.launch { pagerState.animateScrollToPage((s.startPage - 1).coerceIn(0, 603)) }
                                },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (isArabicUi) "${ArabicTextNormalizer.toArabicIndicDigits(s.number)}. سورة ${s.nameAr}"
                                            else "${s.number}. ${s.nameEn} (${s.nameAr})",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = if (isArabicUi) {
                                                "${if (s.isMeccan) "مكية" else "مدنية"} • ${ArabicTextNormalizer.toArabicIndicDigits(s.ayahCount)} آيات"
                                            } else {
                                                "${if (s.isMeccan) "Meccan" else "Medinan"} • ${s.ayahCount} Ayahs"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${strings.pageLabel} ${formatPageNum(s.startPage, isArabicUi)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = BurnishedGold
                                    )
                                }
                            }
                        }
                    }
                    1 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(30) { idx ->
                            val juzNum = idx + 1
                            val startPage = QuranCatalog.juzStartPages[idx]
                            Card(
                                onClick = {
                                    showNavigatorSheet = false
                                    scope.launch { pagerState.animateScrollToPage((startPage - 1).coerceIn(0, 603)) }
                                },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${strings.juzLabel} ${formatPageNum(juzNum, isArabicUi)}", style = MaterialTheme.typography.titleMedium)
                                    Text("${strings.pageLabel} ${formatPageNum(startPage, isArabicUi)}", color = BurnishedGold)
                                }
                            }
                        }
                    }
                    2 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = pageJumpText,
                            onValueChange = { pageJumpText = it.filter { ch -> ch.isDigit() }.take(3) },
                            label = { Text("${strings.pageLabel} (1 - 604)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val target = (pageJumpText.toIntOrNull() ?: activePageNum).coerceIn(1, 604)
                                showNavigatorSheet = false
                                scope.launch { pagerState.animateScrollToPage(target - 1) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isArabicUi) "انتقال إلى الصفحة" else "Jump to Page")
                        }
                    }
                }
            }
        }
    }

    // --- READER PAPER MODE, FONT SCALE & OFFLINE SYNC SHEET ---
    if (showReaderSettingsSheet) {
        var sliderScale by remember(settings.fontScale) { mutableFloatStateOf(fontScale) }
        ModalBottomSheet(onDismissRequest = { showReaderSettingsSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(if (isArabicUi) "إعدادات المصحف والقراءة" else "Mushaf Reading Preferences", style = MaterialTheme.typography.titleLarge)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val labels = listOf(
                        MushafPaperTheme.WHITE to strings.paperWhite,
                        MushafPaperTheme.SEPIA to strings.paperSepia,
                        MushafPaperTheme.DARK to strings.paperDark
                    )
                    labels.forEach { (theme, label) ->
                        FilterChip(
                            selected = paperTheme == theme,
                            onClick = { viewModel.updateSettings(mapOf("mushafPaper" to theme.id)) },
                            label = { Text(label) }
                        )
                    }
                }

                Text("${strings.fontScaleLabel}: ${(sliderScale * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = sliderScale,
                    onValueChange = { sliderScale = it },
                    onValueChangeFinished = {
                        viewModel.updateSettings(mapOf("fontScale" to sliderScale.toDouble()))
                    },
                    valueRange = 0.8f..1.45f
                )

                HorizontalDivider()
                Text(
                    text = "${strings.offlineCachedBadge}: ${formatPageNum(cachedCount, isArabicUi)} / ${formatPageNum(604, isArabicUi)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Button(
                    onClick = {
                        viewModel.syncFullQuranOffline()
                    },
                    enabled = offlineProgress == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.CloudDownload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.downloadOfflineQuran)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // --- DYNAMIC RECITER SELECTOR SHEET ---
    if (showReciterSheet) {
        ModalBottomSheet(onDismissRequest = { showReciterSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(strings.chooseReciter, style = MaterialTheme.typography.titleLarge)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(reciters, key = { it.id }) { reciter ->
                        val selected = audioState.reciter.id == reciter.id
                        Card(
                            onClick = {
                                recitationPlayer.setReciter(reciter)
                                viewModel.updateSettings(mapOf("preferredReciterId" to reciter.id))
                                showReciterSheet = false
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isArabicUi) reciter.nameAr else reciter.nameEn,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isArabicUi) reciter.styleAr else reciter.styleEn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (selected) {
                                    Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // --- A-TO-B MEMORIZATION LOOP DIALOG ---
    if (showLoopDialog) {
        val activeSurah = audioState.currentSurah
        val maxAyah = QuranCatalog.surahByNumber(activeSurah).ayahCount
        var startAyahStr by remember { mutableStateOf(audioState.currentAyah.toString()) }
        var endAyahStr by remember { mutableStateOf((audioState.currentAyah + 2).coerceAtMost(maxAyah).toString()) }
        var repeatsStr by remember { mutableStateOf("5") }

        AlertDialog(
            onDismissRequest = { showLoopDialog = false },
            title = { Text(strings.loopAB) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isArabicUi) "سورة ${QuranCatalog.surahByNumber(activeSurah).nameAr}"
                        else QuranCatalog.surahByNumber(activeSurah).nameEn,
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = startAyahStr,
                        onValueChange = { startAyahStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isArabicUi) "من الآية (أ)" else "Start Ayah (A)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endAyahStr,
                        onValueChange = { endAyahStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isArabicUi) "إلى الآية (ب)" else "End Ayah (B)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = repeatsStr,
                        onValueChange = { repeatsStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isArabicUi) "عدد مرات التكرار" else "Repeat Count") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val sA = (startAyahStr.toIntOrNull() ?: 1).coerceIn(1, maxAyah)
                    val eA = (endAyahStr.toIntOrNull() ?: sA).coerceIn(sA, maxAyah)
                    val rep = (repeatsStr.toIntOrNull() ?: 3).coerceIn(1, 50)
                    recitationPlayer.startAToBLoop(activeSurah, sA, eA, rep, activePageNum)
                    showLoopDialog = false
                }) {
                    Text(if (isArabicUi) "بدء التكرار" else "Start Loop")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    recitationPlayer.clearLoopAndContinue()
                    showLoopDialog = false
                }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

private fun noteTargetDialog(
    ayah: MushafAyahItem,
    isArabicUi: Boolean,
    onOpen: (MushafAyahItem, String) -> Unit
) {
    onOpen(ayah, if (isArabicUi) "تأملات" else "Reflections")
}

@Composable
private fun MushafPageFrame(
    pageNumber: Int,
    pageState: UiState<MushafPageModel>,
    paperTheme: MushafPaperTheme,
    fontScale: Float,
    isArabicUi: Boolean,
    strings: LocalizedStrings,
    activePlayingGlobalAyah: Int,
    selectedGlobalAyah: Int,
    onRetry: () -> Unit,
    onAyahTapped: (MushafAyahItem) -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(paperTheme.bg)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .drawBehind {
                val inset1 = 4.dp.toPx()
                val inset2 = 9.dp.toPx()
                drawRoundRect(
                    color = paperTheme.border.copy(alpha = 0.7f),
                    topLeft = Offset(inset1, inset1),
                    size = Size(size.width - inset1 * 2, size.height - inset1 * 2),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(width = 1.6.dp.toPx())
                )
                drawRoundRect(
                    color = paperTheme.accent.copy(alpha = 0.45f),
                    topLeft = Offset(inset2, inset2),
                    size = Size(size.width - inset2 * 2, size.height - inset2 * 2),
                    cornerRadius = CornerRadius(10.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        when (pageState) {
            is UiState.Loading -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(color = paperTheme.accent)
                    Text(
                        text = if (isArabicUi) "جارٍ تهيئة الصفحة ${ArabicTextNormalizer.toArabicIndicDigits(pageNumber)}..."
                        else "Loading Mushaf Page $pageNumber...",
                        color = paperTheme.fg.copy(alpha = 0.75f)
                    )
                }
            }
            is UiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(strings.networkErrorFallback, color = paperTheme.fg, textAlign = TextAlign.Center)
                    Button(onClick = onRetry) {
                        Text(if (isArabicUi) "إعادة المحاولة" else "Retry")
                    }
                }
            }
            is UiState.Success -> {
                val model = pageState.data
                val firstSurahAr = model.ayahs.firstOrNull()?.surahNameAr ?: ""

                Column(modifier = Modifier.fillMaxSize()) {
                    // Authentic Top Mushaf Metadata Header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${strings.juzLabel} ${formatPageNum(model.juzNumber, isArabicUi)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = paperTheme.accent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${strings.hizbLabel} ${formatPageNum(model.hizbNumber, isArabicUi)} • ${strings.rubLabel} ${formatPageNum(model.rubInHizb, isArabicUi)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = paperTheme.border
                        )
                        Text(
                            text = "سورة $firstSurahAr",
                            style = MaterialTheme.typography.labelLarge,
                            color = paperTheme.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(color = paperTheme.border.copy(alpha = 0.4f))

                    // Scrollable Selectable Interactive Uthmani Script Content
                    SelectionContainer(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp)
                    ) {
                        MushafInteractivePageBody(
                            model = model,
                            paperTheme = paperTheme,
                            fontScale = fontScale,
                            activePlayingGlobalAyah = activePlayingGlobalAyah,
                            selectedGlobalAyah = selectedGlobalAyah,
                            onAyahTapped = onAyahTapped
                        )
                    }

                    HorizontalDivider(color = paperTheme.border.copy(alpha = 0.4f))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onPrevPage, enabled = pageNumber > 1) {
                            Text(if (isArabicUi) "السابقة" else "Prev", color = paperTheme.accent)
                        }
                        Surface(
                            color = paperTheme.cardInner,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, paperTheme.border.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = "${strings.pageLabel} ${formatPageNum(pageNumber, isArabicUi)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = paperTheme.fg,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            )
                        }
                        TextButton(onClick = onNextPage, enabled = pageNumber < 604) {
                            Text(if (isArabicUi) "التالية" else "Next", color = paperTheme.accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MushafInteractivePageBody(
    model: MushafPageModel,
    paperTheme: MushafPaperTheme,
    fontScale: Float,
    activePlayingGlobalAyah: Int,
    selectedGlobalAyah: Int,
    onAyahTapped: (MushafAyahItem) -> Unit
) {
    // Group sequential Ayahs by Surah blocks so Surah headers & Bismillah render cleanly inline
    val blocks = remember(model) {
        val grouped = mutableListOf<Pair<Int, List<MushafAyahItem>>>()
        for (a in model.ayahs) {
            if (grouped.isEmpty() || grouped.last().first != a.surahNumber) {
                grouped.add(a.surahNumber to mutableListOf(a))
            } else {
                (grouped.last().second as MutableList<MushafAyahItem>).add(a)
            }
        }
        grouped
    }

    val fontSizeSp = (23.5f * fontScale).sp
    val lineHeightSp = (44f * fontScale).sp

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        blocks.forEach { (surahNum, surahAyahs) ->
            val startsHere = surahAyahs.firstOrNull()?.numberInSurah == 1
            val meta = QuranCatalog.surahByNumber(surahNum)

            if (startsHere) {
                // Ornamental King Fahd Surah Banner
                Surface(
                    color = paperTheme.cardInner,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, paperTheme.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "سُورَةُ ${meta.nameAr}",
                            fontFamily = AmiriQuranFontFamily,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = paperTheme.accent
                        )
                        Text(
                            text = "${if (meta.isMeccan) "مكية" else "مدنية"} • آياتها ${ArabicTextNormalizer.toArabicIndicDigits(meta.ayahCount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = paperTheme.fg.copy(alpha = 0.7f)
                        )
                    }
                }

                // Bismillah banner (except Surah 1 Al-Fatihah where it is Ayah 1, and Surah 9 At-Tawbah)
                if (surahNum != 1 && surahNum != 9) {
                    Text(
                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                        fontFamily = AmiriQuranFontFamily,
                        fontSize = (22f * fontScale).sp,
                        color = paperTheme.accent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    )
                }
            }

            // Build clickable interactive AnnotatedString for this Surah's verses on the page
            val ayahLookup = remember(surahAyahs) {
                surahAyahs.associateBy { it.globalNumber.toString() }
            }
            val annotatedText: AnnotatedString = remember(
                surahAyahs,
                activePlayingGlobalAyah,
                selectedGlobalAyah,
                paperTheme
            ) {
                buildAnnotatedString {
                    surahAyahs.forEach { ayah ->
                        val isActive = ayah.globalNumber == activePlayingGlobalAyah ||
                            ayah.globalNumber == selectedGlobalAyah
                        val highlightBg = if (isActive) paperTheme.border.copy(alpha = 0.28f) else Color.Transparent

                        pushStringAnnotation(tag = "AYAH", annotation = ayah.globalNumber.toString())
                        withStyle(
                            SpanStyle(
                                color = paperTheme.fg,
                                background = highlightBg
                            )
                        ) {
                            if (ayah.words.isNotEmpty()) {
                                val lineGroups = ayah.words.groupBy { it.lineNumber }
                                val orderedLines = lineGroups.keys.sorted()
                                orderedLines.forEachIndexed { idx, lineNum ->
                                    val tokens = lineGroups[lineNum].orEmpty()
                                    tokens.forEach { token ->
                                        if (token.isEndOfAyah) {
                                            withStyle(
                                                SpanStyle(
                                                    color = paperTheme.accent,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            ) {
                                                append(" ${token.textUthmani} ")
                                            }
                                        } else {
                                            append("${token.textUthmani} ")
                                        }
                                    }
                                    if (idx < orderedLines.lastIndex) {
                                        append("\n")
                                    }
                                }
                            } else {
                                append(ayah.textUthmani)
                                if (ayah.sajdah) {
                                    withStyle(SpanStyle(color = paperTheme.border)) {
                                        append(" ۩")
                                    }
                                }
                                withStyle(
                                    SpanStyle(
                                        color = paperTheme.accent,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append(" ${ArabicTextNormalizer.formatVerseMedallion(ayah.numberInSurah)} ")
                                }
                            }
                        }
                        pop()
                    }
                }
            }

            var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

            Text(
                text = annotatedText,
                style = TextStyle(
                    fontFamily = AmiriQuranFontFamily,
                    fontSize = fontSizeSp,
                    lineHeight = lineHeightSp,
                    textAlign = TextAlign.Center,
                    color = paperTheme.fg
                ),
                onTextLayout = { layoutResult = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(annotatedText) {
                        detectTapGestures { offset ->
                            val result = layoutResult ?: return@detectTapGestures
                            val charOffset = result.getOffsetForPosition(offset)
                            val hit = annotatedText.getStringAnnotations(
                                tag = "AYAH",
                                start = charOffset,
                                end = charOffset
                            ).firstOrNull()
                            if (hit != null) {
                                ayahLookup[hit.item]?.let(onAyahTapped)
                            }
                        }
                    }
            )
        }
    }
}

@Composable
private fun DockedRecitationAudioBar(
    audioState: RecitationPlaybackState,
    isArabicUi: Boolean,
    strings: LocalizedStrings,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onCycleSpeed: () -> Unit,
    onOpenReciters: () -> Unit,
    onOpenLoopDialog: () -> Unit,
    onCachePageAudio: () -> Unit
) {
    val surahMeta = QuranCatalog.surahByNumber(audioState.currentSurah)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenReciters() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.RecordVoiceOver,
                            contentDescription = strings.chooseReciter,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isArabicUi) audioState.reciter.nameAr else audioState.reciter.nameEn,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }
                    val loopNote = if (audioState.playMode == RecitationPlayMode.A_TO_B_LOOP) {
                        " • 🔁 ${audioState.loopStartAyah}-${audioState.loopEndAyah} (${audioState.loopRemaining})"
                    } else ""
                    Text(
                        text = if (isArabicUi) {
                            "${surahMeta.nameAr} • الآية ${ArabicTextNormalizer.toArabicIndicDigits(audioState.currentAyah)}$loopNote"
                        } else {
                            "${surahMeta.nameEn} ${audioState.currentSurah}:${audioState.currentAyah}$loopNote"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrev) {
                        Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous Ayah")
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(onClick = onTogglePlay, modifier = Modifier.testTag("audio_play_pause_button")) {
                            if (audioState.isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (audioState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    IconButton(onClick = onNext) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = "Next Ayah")
                    }
                    if (audioState.isPlaying || audioState.isPaused) {
                        IconButton(onClick = onStop) {
                            Icon(Icons.Rounded.Stop, contentDescription = "Stop")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = false,
                    onClick = onCycleSpeed,
                    label = { Text("${audioState.speed}x") }
                )
                FilterChip(
                    selected = audioState.playMode == RecitationPlayMode.A_TO_B_LOOP,
                    onClick = onOpenLoopDialog,
                    leadingIcon = { Icon(Icons.Rounded.Repeat, null, modifier = Modifier.size(14.dp)) },
                    label = { Text(strings.loopAB) }
                )
                FilterChip(
                    selected = audioState.offlineDownloadingCount > 0,
                    onClick = onCachePageAudio,
                    leadingIcon = { Icon(Icons.Rounded.CloudDownload, null, modifier = Modifier.size(14.dp)) },
                    label = {
                        Text(
                            if (audioState.offlineDownloadingCount > 0) "..."
                            else if (isArabicUi) "حفظ الصفحة" else "Cache Page"
                        )
                    }
                )
            }

            audioState.errorMessage?.let { err ->
                Text(err, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun formatPageNum(value: Int, arabic: Boolean): String =
    if (arabic) ArabicTextNormalizer.toArabicIndicDigits(value) else value.toString()
