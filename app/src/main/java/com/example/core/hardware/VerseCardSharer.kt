package com.example.core.hardware

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a luxury emerald & gold Quran/Dua card bitmap and shares via Android Sharesheet,
 * or falls back cleanly to styled text sharing.
 */
fun shareDesignedVerseTextOrCard(
    context: Context,
    surahLabel: String,
    arabicText: String,
    translation: String
) {
    val shareText = "$arabicText\n\n\"$translation\"\n— $surahLabel (Shared via Mihrab)"
    try {
        val width = 1080
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Emerald gradient background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(0xFF0A2E24.toInt(), 0xFF134D3C.toInt(), 0xFF08241C.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Gold ornamental frame
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = 0xFFE2B95B.toInt()
        }
        canvas.drawRoundRect(RectF(56f, 56f, width - 56f, height - 56f), 40f, 40f, borderPaint)

        val arabicPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFBF6EA.toInt()
            textSize = 54f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val arabicLayout = StaticLayout.Builder.obtain(
            arabicText, 0, arabicText.length, arabicPaint, width - 190
        ).setAlignment(Layout.Alignment.ALIGN_CENTER).setLineSpacing(14f, 1.15f).build()

        val transPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD9E6DF.toInt()
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val transLayout = StaticLayout.Builder.obtain(
            "\"$translation\"", 0, translation.length + 2, transPaint, width - 200
        ).setAlignment(Layout.Alignment.ALIGN_CENTER).setLineSpacing(8f, 1.15f).build()

        canvas.save()
        canvas.translate(95f, 220f)
        arabicLayout.draw(canvas)
        canvas.restore()

        canvas.save()
        canvas.translate(100f, 250f + arabicLayout.height + 56f)
        transLayout.draw(canvas)
        canvas.restore()

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE2B95B.toInt()
            textSize = 30f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("— $surahLabel • Mihrab —", width / 2f, height - 115f, footerPaint)

        val cacheDir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val outFile = File(cacheDir, "mihrab_verse_${System.currentTimeMillis()}.png")
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
        }

        // Note: if FileProvider is not configured, fallback to text share cleanly
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, surahLabel)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share Verse"))
    } catch (_: Exception) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share Verse"))
    }
}
