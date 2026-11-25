package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.hardware.QuranRecitationPlayer
import com.example.core.hardware.RecitationPlaybackState
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalizedStrings
import com.example.data.local.ReciterEntity
import com.example.domain.QuranCatalog
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LocalLiquidGlassPalette
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReciterCloudLibrarySheet(
    reciters: List<ReciterEntity>,
    audioState: RecitationPlaybackState,
    recitationPlayer: QuranRecitationPlayer,
    activeSurahNumber: Int,
    activePageNumber: Int,
    appLanguage: AppLanguage,
    strings: LocalizedStrings,
    onSelectReciter: (ReciterEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalLiquidGlassPalette.current
    val isArabicUi = appLanguage.isArabicUi
    var targetSurahNumber by remember(activeSurahNumber) {
        mutableIntStateOf(activeSurahNumber.coerceIn(1, 114))
    }
    var showSurahPicker by remember { mutableStateOf(false) }
    val targetSurah = remember(targetSurahNumber) { QuranCatalog.surahByNumber(targetSurahNumber) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = glass.glassFillStrong
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appLanguage.pick(
                            "المشايخ والتخزين السحابي",
                            "Reciters & Cloud Audio",
                            "Qorilar va Bulutli Audio"
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = appLanguage.pick(
                            "نزّل السور للاستماع بدون إنترنت وبمشغل شاشة القفل",
                            "Download Surahs for offline playback & lock screen controls",
                            "Oflayn tinglash va qulf ekrani boshqaruvi uchun suralarni yuklang"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Live Bulk Download Banner
            val bulk = audioState.bulkDownload
            if (bulk != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "☁ ${bulk.labelFor(appLanguage)} (${bulk.downloadedAyahs}/${bulk.totalAyahs})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            TextButton(
                                onClick = { recitationPlayer.cancelBulkDownload() },
                                modifier = Modifier.testTag("cancel_bulk_download_button")
                            ) {
                                Text(strings.cancel)
                            }
                        }
                        LinearProgressIndicator(
                            progress = { bulk.fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }

            // Target Surah Picker for Cloud Downloads
            Box {
                OutlinedButton(
                    onClick = { showSurahPicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cloud_surah_picker_button")
                ) {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = appLanguage.pick(
                            "سورة التنزيل المختارة: ${targetSurah.nameAr} (${targetSurah.ayahCount} آية)",
                            "Download target: ${targetSurah.number}. ${targetSurah.nameEn} (${targetSurah.ayahCount} ayahs)",
                            "Yuklash surasi: ${targetSurah.number}. ${targetSurah.nameEn} (${targetSurah.ayahCount} oyat)"
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                }

                DropdownMenu(
                    expanded = showSurahPicker,
                    onDismissRequest = { showSurahPicker = false },
                    modifier = Modifier.heightIn(max = 320.dp)
                ) {
                    QuranCatalog.surahs.forEach { surah ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (isArabicUi) "${surah.number}. ${surah.nameAr} (${surah.ayahCount})"
                                    else "${surah.number}. ${surah.nameEn} (${surah.ayahCount})"
                                )
                            },
                            onClick = {
                                targetSurahNumber = surah.number
                                showSurahPicker = false
                            }
                        )
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(reciters, key = { it.id }) { reciter ->
                    val selected = audioState.reciter.id == reciter.id
                    val cachedCount by recitationPlayer.observeCachedCountForReciter(reciter.id)
                        .collectAsStateWithLifecycle(initialValue = 0)
                    val cachedBytes by recitationPlayer.observeCachedBytesForReciter(reciter.id)
                        .collectAsStateWithLifecycle(initialValue = 0L)
                    val isDownloadingThis = audioState.bulkDownload?.reciterId == reciter.id
                    val mbLabel = String.format(Locale.US, "%.1f MB", cachedBytes / 1_048_576.0)

                    LiquidGlassCard(
                        onClick = {
                            onSelectReciter(reciter)
                            onDismiss()
                        },
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        } else {
                            glass.glassFill
                        },
                        borderColor = if (selected) BurnishedGold else glass.glassBorder,
                        contentPadding = 14.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reciter_card_${reciter.id}")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isArabicUi) reciter.nameAr else reciter.nameEn,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isArabicUi) reciter.styleAr else reciter.styleEn,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = if (reciter.cloudAudioBaseUrl.isNotBlank()) {
                                            BurnishedGold.copy(alpha = 0.18f)
                                        } else {
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                                        },
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = if (reciter.cloudAudioBaseUrl.isNotBlank()) {
                                                appLanguage.pick("☁ سحابة خاصة", "☁ Cloud Storage", "☁ Bulut")
                                            } else {
                                                appLanguage.pick("☁ سحابة التلاوة", "☁ Cloud CDN", "☁ CDN")
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    if (selected) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // Cache stats row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = if (cachedCount > 0) Icons.Rounded.DownloadDone else Icons.Rounded.CloudDone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (cachedCount > 0) {
                                            appLanguage.pick(
                                                "محفوظ: $cachedCount آية ($mbLabel)",
                                                "Cached: $cachedCount ayahs ($mbLabel)",
                                                "Saqlangan: $cachedCount oyat ($mbLabel)"
                                            )
                                        } else {
                                            appLanguage.pick(
                                                "بث سحابي فوري",
                                                "Cloud streaming ready",
                                                "Bulutli oqimga tayyor"
                                            )
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (cachedBytes > 0L) {
                                    IconButton(
                                        onClick = { recitationPlayer.deleteReciterCache(reciter.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = appLanguage.pick("حذف الصوتيات المحفوظة", "Clear cached audio", "Keshni tozalash"),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Action Buttons: Play, Download Selected Surah, Download Juz 'Amma
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onSelectReciter(reciter)
                                        recitationPlayer.playAyah(
                                            surahNumber = targetSurahNumber,
                                            ayahNumber = 1,
                                            pageNumber = activePageNumber,
                                            reciter = reciter
                                        )
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = appLanguage.pick("تلاوة", "Listen", "Tinglash"),
                                        maxLines = 1
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        recitationPlayer.downloadSurahForReciter(reciter, targetSurahNumber)
                                    },
                                    enabled = audioState.bulkDownload == null,
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Icon(Icons.Rounded.CloudDownload, null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (isDownloadingThis) "..." else appLanguage.pick(
                                            "تنزيل السورة",
                                            "Surah",
                                            "Surani yuklash"
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        recitationPlayer.downloadJuzAmmaForReciter(reciter)
                                    },
                                    enabled = audioState.bulkDownload == null,
                                    modifier = Modifier.weight(1.1f)
                                ) {
                                    Text(
                                        text = appLanguage.pick("جزء عمّ", "Juz 'Amma", "Amma juzi"),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
