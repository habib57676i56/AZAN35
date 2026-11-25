package com.example.core.hardware

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalizedStrings
import com.example.data.local.ReciterEntity
import com.example.domain.ArabicTextNormalizer
import com.example.domain.QuranCatalog
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.GlowingGold
import com.example.ui.theme.LuminousMintEmerald
import com.example.ui.theme.SacredEmerald
import java.util.Locale

private val IslandObsidian = Color(0xFF071914)
private val IslandObsidianCard = Color(0xFF0B221B)
private val IslandChipFill = Color(0xFF14352B)

@Composable
fun DynamicIslandReciterOverlay(
    recitationPlayer: QuranRecitationPlayer,
    reciters: List<ReciterEntity>,
    appLanguage: AppLanguage,
    strings: LocalizedStrings,
    onOpenMushaf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val audioState by recitationPlayer.state.collectAsStateWithLifecycle()
    val isVisible = audioState.isPlaying ||
        audioState.isPaused ||
        audioState.isBuffering ||
        audioState.bulkDownload != null

    var isExpanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(220)) + expandVertically(
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            expandFrom = Alignment.Top
        ),
        exit = fadeOut(tween(180)) + shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 4.dp, start = 12.dp, end = 12.dp)
    ) {
        val surahMeta = remember(audioState.currentSurah) {
            QuranCatalog.surahByNumber(audioState.currentSurah)
        }
        val reciterName = if (appLanguage.isArabicUi) {
            audioState.reciter.nameAr
        } else {
            audioState.reciter.nameEn
        }
        val surahTitle = if (appLanguage.isArabicUi) surahMeta.nameAr else surahMeta.nameEn
        val ayahBadge = if (appLanguage.isArabicUi) {
            ArabicTextNormalizer.toArabicIndicDigits(audioState.currentAyah)
        } else {
            audioState.currentAyah.toString()
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    (fadeIn(tween(200)) + expandVertically(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                    )) togetherWith (fadeOut(tween(150)) + shrinkVertically())
                },
                label = "dynamicIslandToggle"
            ) { expanded ->
                if (!expanded) {
                    CompactDynamicIslandPill(
                        audioState = audioState,
                        surahTitle = surahTitle,
                        ayahBadge = ayahBadge,
                        reciterName = reciterName,
                        appLanguage = appLanguage,
                        onTapToggleExpand = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isExpanded = true
                        },
                        onTapPlayPause = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            recitationPlayer.togglePlayPause()
                        },
                        onSwipeNext = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            recitationPlayer.nextAyah()
                        },
                        onSwipePrev = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            recitationPlayer.previousAyah()
                        },
                        onSwipeExpand = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isExpanded = true
                        }
                    )
                } else {
                    ExpandedDynamicIslandCard(
                        audioState = audioState,
                        surahTitle = surahTitle,
                        ayahBadge = ayahBadge,
                        reciterName = reciterName,
                        reciters = reciters,
                        appLanguage = appLanguage,
                        strings = strings,
                        onCollapse = { isExpanded = false },
                        onSwipeNext = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            recitationPlayer.nextAyah()
                        },
                        onSwipePrev = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            recitationPlayer.previousAyah()
                        },
                        onPlayPause = { recitationPlayer.togglePlayPause() },
                        onNext = { recitationPlayer.nextAyah() },
                        onPrev = { recitationPlayer.previousAyah() },
                        onStop = {
                            isExpanded = false
                            recitationPlayer.stop()
                        },
                        onCycleSpeed = {
                            val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                            val idx = speeds.indexOfFirst { kotlin.math.abs(it - audioState.speed) < 0.05f }
                            recitationPlayer.setSpeed(speeds[(idx + 1) % speeds.size])
                        },
                        onSelectReciter = { recitationPlayer.setReciter(it) },
                        onCancelBulkDownload = { recitationPlayer.cancelBulkDownload() },
                        onOpenMushaf = {
                            isExpanded = false
                            onOpenMushaf()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactDynamicIslandPill(
    audioState: RecitationPlaybackState,
    surahTitle: String,
    ayahBadge: String,
    reciterName: String,
    appLanguage: AppLanguage,
    onTapToggleExpand: () -> Unit,
    onTapPlayPause: () -> Unit,
    onSwipeNext: () -> Unit,
    onSwipePrev: () -> Unit,
    onSwipeExpand: () -> Unit
) {
    var horizDrag by remember { mutableFloatStateOf(0f) }
    var vertDrag by remember { mutableFloatStateOf(0f) }
    val bulk = audioState.bulkDownload

    Surface(
        shape = CircleShape,
        color = IslandObsidian,
        border = BorderStroke(1.dp, GlowingGold.copy(alpha = 0.65f)),
        shadowElevation = 10.dp,
        modifier = Modifier
            .widthIn(min = 216.dp, max = 304.dp)
            .height(40.dp)
            .testTag("dynamic_island_pill")
            .pointerInput(audioState.currentSurah, audioState.currentAyah) {
                detectHorizontalDragGestures(
                    onDragStart = { horizDrag = 0f },
                    onHorizontalDrag = { _, dragAmount -> horizDrag += dragAmount },
                    onDragEnd = {
                        if (horizDrag > 72f) {
                            onSwipePrev()
                        } else if (horizDrag < -72f) {
                            onSwipeNext()
                        }
                        horizDrag = 0f
                    }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { vertDrag = 0f },
                    onVerticalDrag = { _, dragAmount -> vertDrag += dragAmount },
                    onDragEnd = {
                        if (vertDrag > 48f) {
                            onSwipeExpand()
                        }
                        vertDrag = 0f
                    }
                )
            }
            .clickable { onTapToggleExpand() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (bulk != null) {
                Icon(
                    imageVector = Icons.Rounded.CloudDownload,
                    contentDescription = null,
                    tint = LuminousMintEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${(bulk.fraction * 100).toInt()}% • ${bulk.labelFor(appLanguage)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            } else {
                if (audioState.isPlaying) {
                    IslandEqualizerBars(color = LuminousMintEmerald)
                } else if (audioState.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = GlowingGold,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = null,
                        tint = GlowingGold,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "$surahTitle • $ayahBadge",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = reciterName,
                        style = MaterialTheme.typography.labelSmall,
                        color = GlowingGold.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    onClick = onTapPlayPause,
                    shape = CircleShape,
                    color = IslandChipFill,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (audioState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = LuminousMintEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpandedDynamicIslandCard(
    audioState: RecitationPlaybackState,
    surahTitle: String,
    ayahBadge: String,
    reciterName: String,
    reciters: List<ReciterEntity>,
    appLanguage: AppLanguage,
    strings: LocalizedStrings,
    onCollapse: () -> Unit,
    onSwipeNext: () -> Unit,
    onSwipePrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onStop: () -> Unit,
    onCycleSpeed: () -> Unit,
    onSelectReciter: (ReciterEntity) -> Unit,
    onCancelBulkDownload: () -> Unit,
    onOpenMushaf: () -> Unit
) {
    var horizDrag by remember { mutableFloatStateOf(0f) }
    var vertDrag by remember { mutableFloatStateOf(0f) }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = IslandObsidianCard,
        border = BorderStroke(1.2.dp, BurnishedGold.copy(alpha = 0.75f)),
        shadowElevation = 16.dp,
        modifier = Modifier
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .pointerInput(audioState.currentSurah, audioState.currentAyah) {
                detectHorizontalDragGestures(
                    onDragStart = { horizDrag = 0f },
                    onHorizontalDrag = { _, dragAmount -> horizDrag += dragAmount },
                    onDragEnd = {
                        if (horizDrag > 72f) onSwipePrev()
                        else if (horizDrag < -72f) onSwipeNext()
                        horizDrag = 0f
                    }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { vertDrag = 0f },
                    onVerticalDrag = { _, dragAmount -> vertDrag += dragAmount },
                    onDragEnd = {
                        if (vertDrag < -48f) onCollapse()
                        vertDrag = 0f
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Sheikh & Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (audioState.isPlaying) {
                        IslandEqualizerBars(color = LuminousMintEmerald)
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.CloudDone,
                            contentDescription = null,
                            tint = GlowingGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reciterName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$surahTitle • ${appLanguage.pick("الآية", "Ayah", "Oyat")} $ayahBadge",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlowingGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = onCycleSpeed,
                        shape = CircleShape,
                        color = IslandChipFill,
                        border = BorderStroke(0.8.dp, GlowingGold.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "${audioState.speed}x",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowUp,
                            contentDescription = "Collapse",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Bulk Cloud Download Banner or Audio Position Bar
            val bulk = audioState.bulkDownload
            if (bulk != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "☁ ${bulk.labelFor(appLanguage)} (${bulk.downloadedAyahs}/${bulk.totalAyahs})",
                            style = MaterialTheme.typography.labelSmall,
                            color = LuminousMintEmerald
                        )
                        IconButton(
                            onClick = onCancelBulkDownload,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = strings.cancel,
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { bulk.fraction },
                        color = LuminousMintEmerald,
                        trackColor = IslandChipFill,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape)
                    )
                }
            } else if (audioState.durationMs > 0L) {
                val frac = (audioState.positionMs.toFloat() / audioState.durationMs.toFloat()).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    LinearProgressIndicator(
                        progress = { frac },
                        color = LuminousMintEmerald,
                        trackColor = IslandChipFill,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatAudioTime(audioState.positionMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        Text(
                            text = formatAudioTime(audioState.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            // Horizontal Reciters Quick Switcher
            if (reciters.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(reciters, key = { it.id }) { item ->
                        val isCurrent = item.id == audioState.reciter.id
                        Surface(
                            onClick = { onSelectReciter(item) },
                            shape = CircleShape,
                            color = if (isCurrent) SacredEmerald else IslandChipFill,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isCurrent) GlowingGold else Color.White.copy(alpha = 0.14f)
                            )
                        ) {
                            Text(
                                text = if (appLanguage.isArabicUi) item.nameAr else item.nameEn,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenMushaf,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("dynamic_island_open_mushaf")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                        contentDescription = strings.mushafReader,
                        tint = GlowingGold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onPrev,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("dynamic_island_prev")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White
                        )
                    }

                    Surface(
                        onClick = onPlayPause,
                        shape = CircleShape,
                        color = LuminousMintEmerald,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("dynamic_island_play_pause")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (audioState.isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = IslandObsidian,
                                    strokeWidth = 2.2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (audioState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = IslandObsidian
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("dynamic_island_next")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White
                        )
                    }
                }

                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("dynamic_island_stop")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = "Stop",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun IslandEqualizerBars(color: Color) {
    val transition = rememberInfiniteTransition(label = "islandEq")
    val barHeights = listOf(0, 140, 260).map { delay ->
        transition.animateFloat(
            initialValue = 0.32f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 460, delayMillis = delay, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eqBar$delay"
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.height(14.dp)
    ) {
        barHeights.forEach { frac ->
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height((14f * frac.value).dp)
                    .background(color, CircleShape)
            )
        }
    }
}

private fun formatAudioTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format(Locale.US, "%d:%02d", mins, secs)
}
