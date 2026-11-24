package com.example.domain

/**
 * Normalizes Arabic Quranic text for diacritic-insensitive & orthographic-tolerant search.
 * Handles Tashkeel, small Alef, Quranic pause marks, Alef variants, Ta Marbuta, and Ya/Alif Maqsura.
 */
object ArabicTextNormalizer {
    private val diacriticsAndMarks = Regex(
        "[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED\u0640]"
    )

    fun normalize(input: String): String {
        if (input.isBlank()) return ""
        val stripped = diacriticsAndMarks.replace(input, "")
        val sb = StringBuilder(stripped.length)
        for (ch in stripped) {
            when (ch) {
                'أ', 'إ', 'آ', 'ٱ', 'ٲ', 'ٳ', 'ٵ' -> sb.append('ا')
                'ة' -> sb.append('ه')
                'ى', 'ي', 'ی', 'ئ' -> sb.append('ي')
                'ؤ' -> sb.append('و')
                'ک' -> sb.append('ك')
                'ـ' -> Unit
                else -> sb.append(ch)
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    fun toArabicIndicDigits(number: Int): String {
        val indic = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        return number.toString().map { ch ->
            if (ch in '0'..'9') indic[ch - '0'] else ch
        }.joinToString("")
    }

    fun formatVerseMedallion(ayahNumber: Int): String =
        "\uFD3F${toArabicIndicDigits(ayahNumber)}\uFD3E"
}
