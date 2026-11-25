package com.example.core.hardware

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

private object BannerBitmapCache {
    val lru = LruCache<String, ImageBitmap>(16)
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
}

fun encodeCompressedBannerBitmapFromUri(
    context: Context,
    uri: Uri,
    maxChars: Int = 140_000
): String? = runCatching {
    val resolver = context.applicationContext.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val rawW = bounds.outWidth
    val rawH = bounds.outHeight
    if (rawW <= 0 || rawH <= 0) {
        val rawBytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching null
        return@runCatching encodeCompressedBannerBitmapToDataUri(rawBytes, maxChars)
    }

    var sampleSize = 1
    while ((rawW / sampleSize) > 1280 || (rawH / sampleSize) > 1280) {
        sampleSize *= 2
    }

    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val sampled = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
        ?: return@runCatching null
    compressBitmapToDataUri(sampled, maxChars)
}.getOrNull()

fun encodeCompressedBannerBitmapToDataUri(rawBytes: ByteArray, maxChars: Int = 140_000): String? {
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
        var sampleSize = 1
        while ((bounds.outWidth / sampleSize) > 1280 || (bounds.outHeight / sampleSize) > 1280) {
            sampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, opts) ?: return null
        compressBitmapToDataUri(decoded, maxChars)
    }.getOrNull()
}

private fun compressBitmapToDataUri(source: Bitmap, maxChars: Int): String? {
    var working = source
    val initialMaxDim = 820f
    val maxSide = maxOf(working.width, working.height).coerceAtLeast(1)
    if (maxSide > initialMaxDim) {
        val scale = initialMaxDim / maxSide.toFloat()
        working = Bitmap.createScaledBitmap(
            working,
            (working.width * scale).toInt().coerceAtLeast(1),
            (working.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    for (dimAttempt in 0..2) {
        var quality = 82
        while (quality >= 28) {
            val out = ByteArrayOutputStream()
            working.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            val candidate = "data:image/jpeg;base64,$b64"
            if (candidate.length <= maxChars) return candidate
            quality -= 12
        }
        if (dimAttempt < 2) {
            working = Bitmap.createScaledBitmap(
                working,
                (working.width * 0.72f).toInt().coerceAtLeast(120),
                (working.height * 0.72f).toInt().coerceAtLeast(80),
                true
            )
        }
    }
    return null
}

private fun normalizeRemoteImageUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    val driveRegex = Regex("https?://drive\\.google\\.com/file/d/([^/?#]+)")
    val match = driveRegex.find(trimmed)
    if (match != null) {
        return "https://drive.google.com/uc?export=download&id=${match.groupValues[1]}"
    }
    return trimmed
}

@Composable
fun RemoteBannerImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 180.dp
) {
    val context = LocalContext.current
    val normalizedUrl = remember(imageUrl) { normalizeRemoteImageUrl(imageUrl) }
    if (normalizedUrl.isBlank()) return

    var bitmap by remember(normalizedUrl) { mutableStateOf<ImageBitmap?>(BannerBitmapCache.lru.get(normalizedUrl)) }
    var isLoading by remember(normalizedUrl) { mutableStateOf(bitmap == null) }
    var loadFailed by remember(normalizedUrl) { mutableStateOf(false) }

    LaunchedEffect(normalizedUrl) {
        if (bitmap != null) {
            isLoading = false
            loadFailed = false
            return@LaunchedEffect
        }
        isLoading = true
        loadFailed = false
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val bytes = when {
                    normalizedUrl.startsWith("data:image", ignoreCase = true) -> {
                        val commaIdx = normalizedUrl.indexOf(',')
                        if (commaIdx > 0) {
                            Base64.decode(normalizedUrl.substring(commaIdx + 1), Base64.DEFAULT)
                        } else null
                    }
                    normalizedUrl.startsWith("content://") -> {
                        context.contentResolver.openInputStream(Uri.parse(normalizedUrl))?.use { it.readBytes() }
                    }
                    normalizedUrl.startsWith("https://") || normalizedUrl.startsWith("http://") -> {
                        val req = Request.Builder()
                            .url(normalizedUrl)
                            .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile) Mihrab/1.0")
                            .build()
                        BannerBitmapCache.httpClient.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) resp.body?.bytes() else null
                        }
                    }
                    else -> null
                }

                bytes?.let { raw ->
                    BitmapFactory.decodeByteArray(raw, 0, raw.size)?.asImageBitmap()
                }
            }.getOrNull()
        }
        isLoading = false
        if (loaded != null) {
            BannerBitmapCache.lru.put(normalizedUrl, loaded)
            bitmap = loaded
        } else {
            loadFailed = true
        }
    }

    val currentBitmap = bitmap
    if (currentBitmap != null) {
        Image(
            bitmap = currentBitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 110.dp, max = maxHeight)
                .clip(RoundedCornerShape(14.dp))
        )
    } else if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 90.dp, max = 110.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        }
    } else if (loadFailed) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp, max = 80.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.BrokenImage,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
