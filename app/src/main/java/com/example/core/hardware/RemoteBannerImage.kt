package com.example.core.hardware

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

private object BannerBitmapCache {
    val lru = LruCache<String, ImageBitmap>(12)
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
}

fun encodeCompressedBannerBitmapToDataUri(rawBytes: ByteArray, maxChars: Int = 6500): String? {
    return runCatching {
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return null
        val maxDim = 560
        val scale = minOf(1f, maxDim.toFloat() / maxOf(decoded.width, decoded.height).coerceAtLeast(1))
        val targetW = (decoded.width * scale).toInt().coerceAtLeast(1)
        val targetH = (decoded.height * scale).toInt().coerceAtLeast(1)
        val scaled = if (scale < 0.99f) {
            Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
        } else decoded

        var quality = 78
        var encodedUri = ""
        while (quality >= 24) {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            val candidate = "data:image/jpeg;base64,$b64"
            encodedUri = candidate
            if (candidate.length <= maxChars) break
            quality -= 12
        }
        encodedUri.takeIf { it.length <= maxChars + 500 }
    }.getOrNull()
}

@Composable
fun RemoteBannerImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 180.dp
) {
    val trimmed = imageUrl.trim()
    if (trimmed.isBlank()) return

    var bitmap by remember(trimmed) { mutableStateOf<ImageBitmap?>(BannerBitmapCache.lru.get(trimmed)) }

    LaunchedEffect(trimmed) {
        if (bitmap != null) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val bytes = if (trimmed.startsWith("data:image", ignoreCase = true)) {
                    val commaIdx = trimmed.indexOf(',')
                    if (commaIdx > 0) {
                        Base64.decode(trimmed.substring(commaIdx + 1), Base64.DEFAULT)
                    } else null
                } else if (trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
                    val req = Request.Builder().url(trimmed).build()
                    BannerBitmapCache.httpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) resp.body?.bytes() else null
                    }
                } else null

                bytes?.let { raw ->
                    BitmapFactory.decodeByteArray(raw, 0, raw.size)?.asImageBitmap()
                }
            }.getOrNull()
        }
        if (loaded != null) {
            BannerBitmapCache.lru.put(trimmed, loaded)
            bitmap = loaded
        }
    }

    bitmap?.let { img ->
        Image(
            bitmap = img,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp, max = maxHeight)
                .clip(RoundedCornerShape(14.dp))
        )
    } ?: if (trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp, max = 96.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        )
    } else Unit
}
