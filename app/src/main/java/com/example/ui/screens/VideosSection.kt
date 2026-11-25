package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.hardware.RemoteBannerImage
import com.example.core.hardware.encodeCompressedBannerBitmapFromUri
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalizedStrings
import com.example.data.firebase.VideoCategory
import com.example.data.firebase.VideoItemDoc
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.LocalLiquidGlassPalette
import com.example.ui.theme.withTabularNums
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val YOUTUBE_ID_REGEX = Regex(
    "(?:youtube\\.com/(?:watch\\?.*v=|embed/|shorts/)|youtu\\.be/)([A-Za-z0-9_\\-]{11})"
)
private val DRIVE_ID_REGEX = Regex("https?://drive\\.google\\.com/file/d/([^/?#]+)")

fun extractYouTubeVideoId(url: String): String? =
    YOUTUBE_ID_REGEX.find(url.trim())?.groupValues?.getOrNull(1)

fun normalizePlayableVideoUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    val driveMatch = DRIVE_ID_REGEX.find(trimmed)
    if (driveMatch != null) {
        return "https://drive.google.com/uc?export=download&id=${driveMatch.groupValues[1]}"
    }
    return trimmed
}

fun isDirectPlayableVideoUrl(rawUrl: String): Boolean {
    val trimmed = rawUrl.trim()
    if (extractYouTubeVideoId(trimmed) != null) return false
    val lower = trimmed.lowercase()
    return lower.contains(".mp4") ||
        lower.contains(".webm") ||
        lower.contains(".m4v") ||
        lower.contains(".mov") ||
        lower.contains(".m3u8") ||
        lower.contains(".3gp") ||
        lower.contains("drive.google.com/uc")
}

fun resolveVideoThumbnailUrl(video: VideoItemDoc): String {
    val custom = video.thumbnailUrl.trim()
    if (custom.isNotBlank()) return custom
    val ytId = extractYouTubeVideoId(video.videoUrl)
    return if (!ytId.isNullOrBlank()) {
        "https://img.youtube.com/vi/$ytId/hqdefault.jpg"
    } else {
        ""
    }
}

private fun VideoItemDoc.localizedTitle(language: AppLanguage): String =
    language.pick(
        titleAr.ifBlank { titleEn },
        titleEn.ifBlank { titleAr },
        titleUz.ifBlank { titleEn.ifBlank { titleAr } }
    )

private fun VideoItemDoc.localizedDescription(language: AppLanguage): String =
    language.pick(
        descriptionAr.ifBlank { descriptionEn },
        descriptionEn.ifBlank { descriptionAr },
        descriptionUz.ifBlank { descriptionEn.ifBlank { descriptionAr } }
    )

@Composable
fun VideosHubContent(
    videos: List<VideoItemDoc>,
    videosEnabled: Boolean,
    appLanguage: AppLanguage,
    isAdmin: Boolean = false,
    onOpenAdminVideos: (() -> Unit)? = null
) {
    val glass = LocalLiquidGlassPalette.current
    var selectedCategory by remember { mutableStateOf("ALL") }
    var playingVideo by remember { mutableStateOf<VideoItemDoc?>(null) }

    val filteredVideos = remember(videos, selectedCategory) {
        if (selectedCategory == "ALL") videos else videos.filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LiquidGlassCard(
            shape = RoundedCornerShape(24.dp),
            containerColor = glass.glassFillStrong,
            borderColor = BurnishedGold.copy(alpha = 0.45f),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("videos_section_header")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.OndemandVideo,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = appLanguage.pick(
                                "الفيديوهات الإسلامية وكرتون الأطفال",
                                "Islamic Videos & Cartoons",
                                "Islomiy videolar va multfilmlar"
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = appLanguage.pick(
                                "مقاطع تشرح الإسلام، قصص الأنبياء، دروس القرآن، وكرتون هادف للصغار",
                                "Explaining Islam, Quran lessons, Prophet stories & Islamic cartoons",
                                "Islomni tushuntirish, Qur'on darslari va bolalar multfilmlari"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isAdmin && onOpenAdminVideos != null) {
                OutlinedButton(
                    onClick = onOpenAdminVideos,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        appLanguage.pick(
                            "إرسال فيديو جديد من لوحة الإدارة",
                            "Publish Video in Admin Panel",
                            "Admin paneldan video qo'shish"
                        )
                    )
                }
            }
        }

        if (!videosEnabled) {
            LiquidGlassCard(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = appLanguage.pick(
                        "قسم الفيديوهات متوقف مؤقتاً من لوحة الإدارة.",
                        "Videos section is temporarily paused by Admin.",
                        "Video bo'limi administrator tomonidan vaqtincha o'chirilgan."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                LiquidGlassPill(
                    selected = selectedCategory == "ALL",
                    onClick = { selectedCategory = "ALL" },
                    label = appLanguage.pick("الكل (${videos.size})", "All (${videos.size})", "Barchasi (${videos.size})"),
                    modifier = Modifier.testTag("videos_filter_all")
                )
            }
            items(VideoCategory.entries) { cat ->
                val count = videos.count { it.category == cat.id }
                LiquidGlassPill(
                    selected = selectedCategory == cat.id,
                    onClick = { selectedCategory = cat.id },
                    label = "${cat.labelFor(appLanguage)} ($count)",
                    modifier = Modifier.testTag("videos_filter_${cat.id}")
                )
            }
        }

        if (filteredVideos.isEmpty()) {
            LiquidGlassCard(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("videos_empty_state")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SmartDisplay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = appLanguage.pick(
                            "لا توجد فيديوهات منشورة في هذا التصنيف بعد",
                            "No videos published in this category yet",
                            "Ushbu bo'limda hali videolar yo'q"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = appLanguage.pick(
                            "يمكن للمشرف إرسال فيديوهات شرح الإسلام وكرتون الأطفال من لوحة الإدارة وستظهر هنا فوراً.",
                            "Admins can publish Islamic explainer videos and cartoons from the Admin Dashboard.",
                            "Adminlar boshqaruv panelidan video va multfilmlarni qo'shishlari mumkin."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            filteredVideos.forEach { video ->
                VideoCatalogCard(
                    video = video,
                    appLanguage = appLanguage,
                    onWatchClick = { playingVideo = video }
                )
            }
        }
    }

    playingVideo?.let { video ->
        VideoPlayerDialog(
            video = video,
            appLanguage = appLanguage,
            onDismiss = { playingVideo = null }
        )
    }
}

@Composable
private fun VideoCatalogCard(
    video: VideoItemDoc,
    appLanguage: AppLanguage,
    onWatchClick: () -> Unit
) {
    val glass = LocalLiquidGlassPalette.current
    val thumbUrl = remember(video.thumbnailUrl, video.videoUrl) { resolveVideoThumbnailUrl(video) }
    val catLabel = VideoCategory.fromId(video.category).labelFor(appLanguage)
    val title = video.localizedTitle(appLanguage)
    val desc = video.localizedDescription(appLanguage)

    LiquidGlassCard(
        onClick = onWatchClick,
        shape = RoundedCornerShape(24.dp),
        containerColor = glass.glassFillStrong,
        borderColor = if (video.category == VideoCategory.ISLAMIC_CARTOON.id) {
            BurnishedGold.copy(alpha = 0.45f)
        } else {
            glass.glassBorder
        },
        contentPadding = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("video_card_${video.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            BurnishedGold.copy(alpha = 0.16f)
                        )
                    )
                )
        ) {
            if (thumbUrl.isNotBlank()) {
                RemoteBannerImage(
                    imageUrl = thumbUrl,
                    contentDescription = title,
                    maxHeight = 200.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SmartDisplay,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            // Center Play Pill
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.62f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = appLanguage.pick("تشغيل", "Play", "Ijro etish"),
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            if (video.durationLabel.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = video.durationLabel,
                            style = MaterialTheme.typography.labelSmall.withTabularNums(),
                            color = Color.White
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
            ) {
                Text(
                    text = catLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            TextButton(onClick = onWatchClick) {
                Icon(Icons.Rounded.PlayCircle, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(appLanguage.pick("مشاهدة الآن", "Watch Now", "Ko'rish"))
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (desc.isNotBlank()) {
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun VideoPlayerDialog(
    video: VideoItemDoc,
    appLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val glass = LocalLiquidGlassPalette.current
    val normalizedUrl = remember(video.videoUrl) { normalizePlayableVideoUrl(video.videoUrl) }
    val isDirectStream = remember(video.videoUrl) { isDirectPlayableVideoUrl(video.videoUrl) }
    val thumbUrl = remember(video.thumbnailUrl, video.videoUrl) { resolveVideoThumbnailUrl(video) }
    val title = video.localizedTitle(appLanguage)
    val desc = video.localizedDescription(appLanguage)

    fun openExternally() {
        val target = video.videoUrl.trim().let {
            if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it"
        }
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(target)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LiquidGlassCard(
            shape = RoundedCornerShape(26.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            borderColor = glass.glassBorder,
            contentPadding = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("video_player_dialog")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = VideoCategory.fromId(video.category).labelFor(appLanguage),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            if (isDirectStream) {
                var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
                var isPlaying by remember { mutableStateOf(true) }
                var isBuffering by remember { mutableStateOf(true) }
                var playbackError by remember { mutableStateOf(false) }
                var positionMs by remember { mutableIntStateOf(0) }
                var durationMs by remember { mutableIntStateOf(0) }

                LaunchedEffect(videoViewRef, isPlaying) {
                    while (true) {
                        val vv = videoViewRef
                        if (vv != null && vv.duration > 0) {
                            durationMs = vv.duration
                            positionMs = vv.currentPosition
                            isPlaying = vv.isPlaying
                        }
                        delay(450L)
                    }
                }

                DisposableEffect(normalizedUrl) {
                    onDispose {
                        runCatching { videoViewRef?.stopPlayback() }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(Uri.parse(normalizedUrl))
                                setOnPreparedListener { mp ->
                                    isBuffering = false
                                    playbackError = false
                                    mp.isLooping = false
                                    start()
                                    isPlaying = true
                                }
                                setOnErrorListener { _, _, _ ->
                                    isBuffering = false
                                    playbackError = true
                                    true
                                }
                                setOnCompletionListener {
                                    isPlaying = false
                                }
                                videoViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isBuffering && !playbackError) {
                        CircularProgressIndicator(color = Color.White)
                    }
                    if (playbackError) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = appLanguage.pick(
                                    "تعذر التشغيل المباشر داخل المشغل؛ افتحه بالمشغل الخارجي",
                                    "Direct playback failed; open in external player",
                                    "To'g'ridan-to'g'ri ijro etilmadi; tashqi pleyerda oching"
                                ),
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(onClick = { openExternally() }) {
                                Text(appLanguage.pick("فتح بالمشغل الخارجي", "Open Externally", "Tashqi pleyerda ochish"))
                            }
                        }
                    }
                }

                if (!playbackError) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val vv = videoViewRef ?: return@IconButton
                                if (vv.isPlaying) {
                                    vv.pause()
                                    isPlaying = false
                                } else {
                                    vv.start()
                                    isPlaying = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                            onValueChange = { ratio ->
                                if (durationMs > 0) {
                                    val target = (ratio * durationMs).toInt()
                                    videoViewRef?.seekTo(target)
                                    positionMs = target
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatMillisLabel(positionMs),
                            style = MaterialTheme.typography.labelSmall.withTabularNums(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                if (thumbUrl.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { openExternally() }
                    ) {
                        RemoteBannerImage(
                            imageUrl = thumbUrl,
                            contentDescription = title,
                            maxHeight = 220.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.68f),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                }
                Button(
                    onClick = { openExternally() },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        appLanguage.pick(
                            "تشغيل الفيديو الآن",
                            "Watch Video Now",
                            "Videoni hozir tomosha qilish"
                        )
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (desc.isNotBlank()) {
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isDirectStream) {
                OutlinedButton(
                    onClick = { openExternally() },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(appLanguage.pick("فتح في مشغل خارجي", "Open in External Player", "Tashqi pleyerda ochish"))
                }
            }
        }
    }
}

private fun formatMillisLabel(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format("%02d:%02d", mins, secs)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideosCrudTab(
    videos: List<VideoItemDoc>,
    appLanguage: AppLanguage,
    strings: LocalizedStrings,
    onUpsert: (VideoItemDoc) -> Unit,
    onDelete: (String) -> Unit
) {
    val glass = LocalLiquidGlassPalette.current
    var editing by remember { mutableStateOf<VideoItemDoc?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LiquidGlassCard(shape = RoundedCornerShape(24.dp), containerColor = glass.glassFillStrong) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.OndemandVideo, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = appLanguage.pick(
                            "إدارة وإرسال الفيديوهات الإسلامية (${videos.size})",
                            "Islamic Videos & Cartoons (${videos.size})",
                            "Islomiy videolar va multfilmlar (${videos.size})"
                        ),
                        style = MaterialTheme.typography.titleLarge.withTabularNums(),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = appLanguage.pick(
                        "أرسل روابط يوتيوب أو Google Drive أو MP4 مع صورة مصغّرة لشرح الإسلام أو كرتون الأطفال، وستظهر فوراً في قسم الفيديوهات.",
                        "Publish YouTube, Google Drive, or direct MP4 video links for Islamic explainers and cartoons.",
                        "Islomni tushuntirish va bolalar multfilmlari uchun YouTube, Drive yoki MP4 havolalarni yuboring."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        editing = VideoItemDoc(
                            id = "vid_${System.currentTimeMillis()}",
                            category = VideoCategory.EXPLAIN_ISLAM.id,
                            sortOrder = (videos.size + 1).toLong(),
                            active = true
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_add_video_button")
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        appLanguage.pick(
                            "إرسال فيديو / كرتون إسلامي جديد",
                            "Send New Islamic Video / Cartoon",
                            "Yangi video / multfilm yuborish"
                        )
                    )
                }
            }
        }

        items(videos, key = { it.id }) { v ->
            val thumb = resolveVideoThumbnailUrl(v)
            LiquidGlassCard(
                shape = RoundedCornerShape(22.dp),
                borderColor = if (v.active) glass.glassBorder else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_video_card_${v.id}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = VideoCategory.fromId(v.category).labelFor(appLanguage),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = v.active,
                            onCheckedChange = { onUpsert(v.copy(active = it)) }
                        )
                        IconButton(onClick = { editing = v }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { onDelete(v.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                if (thumb.isNotBlank()) {
                    RemoteBannerImage(
                        imageUrl = thumb,
                        contentDescription = v.localizedTitle(appLanguage),
                        maxHeight = 140.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(
                    text = v.localizedTitle(appLanguage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (v.localizedDescription(appLanguage).isNotBlank()) {
                    Text(
                        text = v.localizedDescription(appLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = v.videoUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    editing?.let { current ->
        VideoEditorDialog(
            initial = current,
            appLanguage = appLanguage,
            strings = strings,
            onDismiss = { editing = null },
            onSave = {
                onUpsert(it)
                editing = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoEditorDialog(
    initial: VideoItemDoc,
    appLanguage: AppLanguage,
    strings: LocalizedStrings,
    onDismiss: () -> Unit,
    onSave: (VideoItemDoc) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var category by remember { mutableStateOf(initial.category) }
    var titleAr by remember { mutableStateOf(initial.titleAr) }
    var titleEn by remember { mutableStateOf(initial.titleEn) }
    var titleUz by remember { mutableStateOf(initial.titleUz) }
    var descAr by remember { mutableStateOf(initial.descriptionAr) }
    var descEn by remember { mutableStateOf(initial.descriptionEn) }
    var videoUrl by remember { mutableStateOf(initial.videoUrl) }
    var thumbnailUrl by remember { mutableStateOf(initial.thumbnailUrl) }
    var durationLabel by remember { mutableStateOf(initial.durationLabel) }
    var sortOrderText by remember { mutableStateOf(initial.sortOrder.toString()) }
    var active by remember { mutableStateOf(initial.active) }
    var thumbNotice by remember { mutableStateOf<String?>(null) }

    val thumbPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            thumbNotice = appLanguage.pick("جاري معالجة الصورة...", "Compressing thumbnail...", "Rasm tayyorlanmoqda...")
            scope.launch {
                val encoded = withContext(Dispatchers.IO) {
                    encodeCompressedBannerBitmapFromUri(context, uri, maxChars = 135_000)
                }
                if (!encoded.isNullOrBlank()) {
                    thumbnailUrl = encoded
                    thumbNotice = appLanguage.pick("تم اختيار الصورة المصغّرة ✓", "Thumbnail ready ✓", "Muqova rasmi tayyor ✓")
                } else {
                    thumbNotice = appLanguage.pick("تعذر ضغط الصورة؛ جرّب صورة أخرى", "Could not compress image", "Rasmni siqib bo'lmadi")
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                appLanguage.pick(
                    "تفاصيل الفيديو الإسلامي",
                    "Islamic Video Details",
                    "Islomiy video ma'lumotlari"
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    appLanguage.pick("التصنيف", "Category", "Bo'lim"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VideoCategory.entries.forEach { cat ->
                        LiquidGlassPill(
                            selected = category == cat.id,
                            onClick = { category = cat.id },
                            label = cat.labelFor(appLanguage)
                        )
                    }
                }
                OutlinedTextField(
                    value = titleAr,
                    onValueChange = { titleAr = it },
                    label = { Text("عنوان الفيديو بالعربية *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_video_title_ar")
                )
                OutlinedTextField(
                    value = titleEn,
                    onValueChange = { titleEn = it },
                    label = { Text("Video Title (English)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = titleUz,
                    onValueChange = { titleUz = it },
                    label = { Text("Video nomi (Oʻzbekcha)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descAr,
                    onValueChange = { descAr = it },
                    label = { Text("وصف مختصر (AR)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descEn,
                    onValueChange = { descEn = it },
                    label = { Text("Short Description (EN)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    leadingIcon = { Icon(Icons.Rounded.Link, null) },
                    label = { Text("رابط الفيديو (YouTube / MP4 / Drive) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_video_url_input")
                )
                OutlinedButton(
                    onClick = {
                        thumbPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Image, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(appLanguage.pick("اختيار صورة مصغّرة من الاستوديو", "Pick Thumbnail from Gallery", "Galereyadan muqova tanlash"))
                }
                OutlinedTextField(
                    value = thumbnailUrl,
                    onValueChange = { thumbnailUrl = it },
                    label = { Text(appLanguage.pick("أو رابط صورة مصغّرة (تلقائي ليوتيوب)", "Thumbnail URL (auto for YouTube)", "Muqova havolasi")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!thumbNotice.isNullOrBlank()) {
                    Text(thumbNotice!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = durationLabel,
                        onValueChange = { if (it.length <= 20) durationLabel = it },
                        label = { Text(appLanguage.pick("المدة (مثل 05:30)", "Duration", "Davomiyligi")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sortOrderText,
                        onValueChange = { s -> sortOrderText = s.filter { it.isDigit() }.take(4) },
                        label = { Text(appLanguage.pick("الترتيب", "Order", "Tartib")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(appLanguage.pick("منشور للمستخدمين", "Published (Active)", "Faol"))
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val safeTitleAr = titleAr.trim().ifBlank { titleEn.trim() }
                    onSave(
                        initial.copy(
                            category = category,
                            titleAr = safeTitleAr,
                            titleEn = titleEn.trim().ifBlank { safeTitleAr },
                            titleUz = titleUz.trim(),
                            descriptionAr = descAr.trim(),
                            descriptionEn = descEn.trim(),
                            videoUrl = videoUrl.trim(),
                            thumbnailUrl = thumbnailUrl.trim(),
                            durationLabel = durationLabel.trim(),
                            sortOrder = (sortOrderText.toLongOrNull() ?: 1L).coerceIn(0L, 10000L),
                            active = active
                        )
                    )
                },
                enabled = videoUrl.trim().isNotBlank() && (titleAr.trim().isNotBlank() || titleEn.trim().isNotBlank()),
                modifier = Modifier.testTag("admin_save_video_button")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
