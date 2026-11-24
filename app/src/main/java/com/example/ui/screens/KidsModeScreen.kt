package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.hardware.QuranTtsAudioPlayer
import com.example.domain.IslamicLibraryData
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.BurnishedGold

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KidsModeScreen(
    audioPlayer: QuranTtsAudioPlayer,
    onExitKidsMode: () -> Unit
) {
    val starsEarned = remember { mutableStateListOf(true, false, false, false) }
    val countStars = starsEarned.count { it }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.ChildCare, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Mihrab Kids Sanctuary", style = MaterialTheme.typography.headlineMedium)
                            }
                            OutlinedButton(onClick = onExitKidsMode) {
                                Text("Exit Kids Mode")
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(countStars) {
                                Icon(Icons.Rounded.Star, null, tint = BurnishedGold, modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("$countStars / 4 Stars Earned Today!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("My Daily Good Deeds", style = MaterialTheme.typography.titleLarge)
                        val deeds = listOf(
                            "Said Bismillah before eating",
                            "Prayed with family",
                            "Recited Surah Al-Ikhlas",
                            "Spoke kindly to parents"
                        )
                        deeds.forEachIndexed { idx, deed ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = starsEarned[idx], onCheckedChange = { starsEarned[idx] = it })
                                Text(deed, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Tap Arabic Letters to Hear!", style = MaterialTheme.typography.titleLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            IslamicLibraryData.nooraniaLessons.forEach { l ->
                                Surface(
                                    onClick = { audioPlayer.speak(l.name, l.letter, preferArabic = true) },
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(l.letter, fontFamily = AmiriFontFamily, fontSize = 34.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(l.name.substringBefore(" "), style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Bedtime Prophet Stories", style = MaterialTheme.typography.titleLarge)
                        IslamicLibraryData.prophetsStories.take(4).forEach { story ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(story.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(story.keyLesson, style = MaterialTheme.typography.bodyMedium)
                                }
                                Button(onClick = {
                                    audioPlayer.speak(story.name, "${story.name}. ${story.summary}", preferArabic = false)
                                }) {
                                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Listen")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
