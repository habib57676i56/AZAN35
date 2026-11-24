package com.example.data.quran

import android.util.Log
import com.example.data.local.MushafDao
import com.example.data.local.MushafPageEntity
import com.example.data.local.QuranAyahIndexEntity
import com.example.domain.ArabicTextNormalizer
import com.example.domain.MushafAyahItem
import com.example.domain.MushafPageModel
import com.example.domain.MushafWordToken
import com.example.domain.QuranCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class QuranSearchHit(
    val globalAyahNumber: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val pageNumber: Int,
    val surahNameAr: String,
    val surahNameEn: String,
    val uthmaniText: String
)

class MushafRepository(
    private val mushafDao: MushafDao
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var quranPageApiBase: String = "https://api.quran.com/api/v4"

    @Volatile
    private var quranFallbackApiBase: String = "https://api.alquran.cloud/v1"

    fun setApiSources(primaryBase: String, fallbackBase: String) {
        val cleanPrimary = primaryBase.trim().trimEnd('/')
        val cleanFallback = fallbackBase.trim().trimEnd('/')
        if (cleanPrimary.startsWith("https://")) {
            quranPageApiBase = cleanPrimary
        }
        if (cleanFallback.startsWith("https://")) {
            quranFallbackApiBase = cleanFallback
        }
    }

    val cachedPageCount: Flow<Int> = mushafDao.observeCachedPageCount()

    suspend fun getPage(pageNumber: Int): Result<MushafPageModel> = withContext(Dispatchers.IO) {
        val safePage = pageNumber.coerceIn(1, 604)
        try {
            val cached = mushafDao.getPage(safePage)
            if (cached != null) {
                return@withContext Result.success(entityToModel(cached))
            }

            // Try Quran.com v4 first (includes authentic 15-line Madinah Mushaf line breaks)
            val fetched = runCatching { fetchFromQuranCom(safePage) }
                .getOrNull()
                ?: fetchFromAlQuranCloud(safePage)

            mushafDao.insertPage(modelToEntity(fetched))
            mushafDao.insertAyahIndexBatch(fetched.ayahs.map { it.toIndexEntity() })
            Result.success(fetched)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Page $safePage fetch failed", e)
            val starter = QuranCatalog.offlineStarterPage(safePage)
            if (starter != null) {
                Result.success(starter)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun prefetchPages(centerPage: Int) = withContext(Dispatchers.IO) {
        val targets = listOf(centerPage + 1, centerPage + 2, centerPage - 1)
            .filter { it in 1..604 }
        for (p in targets) {
            try {
                if (mushafDao.getPage(p) == null) {
                    val fetched = runCatching { fetchFromQuranCom(p) }
                        .getOrNull() ?: runCatching { fetchFromAlQuranCloud(p) }.getOrNull()
                    if (fetched != null) {
                        mushafDao.insertPage(modelToEntity(fetched))
                        mushafDao.insertAyahIndexBatch(fetched.ayahs.map { it.toIndexEntity() })
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Downloads the entire 604-page Quran once (~1.8 MB) and indexes all 6,236 Ayahs in Room SQLite
     * for instant offline reading and normalized Arabic search.
     */
    suspend fun downloadFullQuranOffline(onProgress: (Float) -> Unit): Result<Int> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.1f)
            val url = "${quranFallbackApiBase}/quran/quran-uthmani"
            val req = Request.Builder().url(url).build()
            val body = httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                resp.body?.string() ?: throw IllegalStateException("Empty Quran payload")
            }
            onProgress(0.45f)

            val root = JSONObject(body)
            val surahsJson = root.getJSONObject("data").getJSONArray("surahs")
            val pageBuckets = HashMap<Int, MutableList<MushafAyahItem>>(604)
            val allAyahEntities = ArrayList<QuranAyahIndexEntity>(6236)

            for (sIdx in 0 until surahsJson.length()) {
                val sObj = surahsJson.getJSONObject(sIdx)
                val surahNum = sObj.getInt("number")
                val meta = QuranCatalog.surahByNumber(surahNum)
                val ayahsArr = sObj.getJSONArray("ayahs")
                for (aIdx in 0 until ayahsArr.length()) {
                    val aObj = ayahsArr.getJSONObject(aIdx)
                    val globalNum = aObj.getInt("number")
                    val numInSurah = aObj.getInt("numberInSurah")
                    val page = aObj.getInt("page").coerceIn(1, 604)
                    val juz = aObj.getInt("juz")
                    val hizbQuarter = aObj.optInt("hizbQuarter", 1)
                    val sajdah = parseSajdaField(aObj.opt("sajda"))
                    val rawText = cleanPrependedBismillah(
                        raw = aObj.getString("text"),
                        surahNumber = surahNum,
                        ayahInSurah = numInSurah
                    )
                    val item = MushafAyahItem(
                        globalNumber = globalNum,
                        surahNumber = surahNum,
                        surahNameAr = meta.nameAr,
                        surahNameEn = meta.nameEn,
                        numberInSurah = numInSurah,
                        textUthmani = rawText,
                        pageNumber = page,
                        juzNumber = juz,
                        hizbQuarter = hizbQuarter,
                        sajdah = sajdah,
                        words = emptyList()
                    )
                    pageBuckets.getOrPut(page) { ArrayList() }.add(item)
                    allAyahEntities.add(item.toIndexEntity())
                }
            }
            onProgress(0.75f)

            val entities = (1..604).mapNotNull { pageNum ->
                val list = pageBuckets[pageNum] ?: return@mapNotNull null
                val first = list.first()
                modelToEntity(
                    MushafPageModel(
                        pageNumber = pageNum,
                        juzNumber = first.juzNumber,
                        hizbQuarter = first.hizbQuarter,
                        ayahs = list
                    )
                )
            }
            mushafDao.insertPages(entities)
            mushafDao.insertAyahIndexBatch(allAyahEntities)
            onProgress(1.0f)
            Result.success(entities.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Offline full Quran sync failed", e)
            Result.failure(e)
        }
    }

    suspend fun searchQuran(rawQuery: String): List<QuranSearchHit> = withContext(Dispatchers.IO) {
        val trimmed = rawQuery.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        val normalized = ArabicTextNormalizer.normalize(trimmed)
        val localHits = mushafDao.searchAyahs(normalized, trimmed).map {
            QuranSearchHit(
                globalAyahNumber = it.globalAyahNumber,
                surahNumber = it.surahNumber,
                ayahNumber = it.ayahNumber,
                pageNumber = it.pageNumber,
                surahNameAr = it.surahNameAr,
                surahNameEn = it.surahNameEn,
                uthmaniText = it.uthmaniText
            )
        }
        val cachedPages = mushafDao.getCachedPageCount()
        if (cachedPages >= 604 || localHits.size >= 15) {
            return@withContext localHits
        }

        // Also query online Quran search if full Mushaf isn't downloaded yet
        val onlineHits = runCatching { searchOnlineAlQuranCloud(normalized.ifBlank { trimmed }) }
            .getOrDefault(emptyList())
        val seen = HashSet<Int>(localHits.map { it.globalAyahNumber })
        val merged = ArrayList(localHits)
        for (hit in onlineHits) {
            if (seen.add(hit.globalAyahNumber)) merged.add(hit)
        }
        merged.take(50)
    }

    private fun fetchFromQuranCom(page: Int): MushafPageModel {
        val url = "${quranPageApiBase}/verses/by_page/$page" +
            "?language=ar&words=true" +
            "&fields=text_uthmani,verse_key,page_number,juz_number,hizb_number,rub_el_hizb_number,sajdah_number" +
            "&word_fields=text_uthmani,line_number,char_type_name"
        val req = Request.Builder().url(url).build()
        val body = httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("Quran.com HTTP ${resp.code}")
            resp.body?.string() ?: throw IllegalStateException("Empty Quran.com body")
        }
        val verses = JSONObject(body).getJSONArray("verses")
        val ayahs = ArrayList<MushafAyahItem>(verses.length())
        for (i in 0 until verses.length()) {
            val v = verses.getJSONObject(i)
            val keyParts = v.getString("verse_key").split(":")
            val surahNum = keyParts.getOrNull(0)?.toIntOrNull() ?: 1
            val ayahInSurah = keyParts.getOrNull(1)?.toIntOrNull() ?: 1
            val meta = QuranCatalog.surahByNumber(surahNum)
            val globalNum = QuranCatalog.globalAyahNumber(surahNum, ayahInSurah)
            val juz = v.optInt("juz_number", QuranCatalog.juzForPage(page))
            val rub = v.optInt("rub_el_hizb_number", 1)
            val sajdah = !v.isNull("sajdah_number")
            val textUthmani = v.getString("text_uthmani")

            val wordsArr = v.optJSONArray("words")
            val words = ArrayList<MushafWordToken>()
            if (wordsArr != null) {
                for (wIdx in 0 until wordsArr.length()) {
                    val wObj = wordsArr.getJSONObject(wIdx)
                    val charType = wObj.optString("char_type_name", "word")
                    val line = wObj.optInt("line_number", 1).coerceIn(1, 15)
                    val wText = wObj.optString("text_uthmani", "")
                    if (charType == "end") {
                        words.add(
                            MushafWordToken(
                                textUthmani = ArabicTextNormalizer.formatVerseMedallion(ayahInSurah),
                                lineNumber = line,
                                isEndOfAyah = true
                            )
                        )
                    } else if (wText.isNotBlank()) {
                        words.add(MushafWordToken(textUthmani = wText, lineNumber = line, isEndOfAyah = false))
                    }
                }
            }

            ayahs.add(
                MushafAyahItem(
                    globalNumber = globalNum,
                    surahNumber = surahNum,
                    surahNameAr = meta.nameAr,
                    surahNameEn = meta.nameEn,
                    numberInSurah = ayahInSurah,
                    textUthmani = textUthmani,
                    pageNumber = page,
                    juzNumber = juz,
                    hizbQuarter = rub,
                    sajdah = sajdah,
                    words = words
                )
            )
        }
        val first = ayahs.firstOrNull()
        return MushafPageModel(
            pageNumber = page,
            juzNumber = first?.juzNumber ?: QuranCatalog.juzForPage(page),
            hizbQuarter = first?.hizbQuarter ?: 1,
            ayahs = ayahs
        )
    }

    private fun fetchFromAlQuranCloud(page: Int): MushafPageModel {
        val url = "${quranFallbackApiBase}/page/$page/quran-uthmani"
        val req = Request.Builder().url(url).build()
        val body = httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("AlQuranCloud HTTP ${resp.code}")
            resp.body?.string() ?: throw IllegalStateException("Empty AlQuranCloud body")
        }
        val ayahsArr = JSONObject(body).getJSONObject("data").getJSONArray("ayahs")
        val ayahs = ArrayList<MushafAyahItem>(ayahsArr.length())
        for (i in 0 until ayahsArr.length()) {
            val aObj = ayahsArr.getJSONObject(i)
            val sObj = aObj.getJSONObject("surah")
            val surahNum = sObj.getInt("number")
            val meta = QuranCatalog.surahByNumber(surahNum)
            val numInSurah = aObj.getInt("numberInSurah")
            val cleanText = cleanPrependedBismillah(
                raw = aObj.getString("text"),
                surahNumber = surahNum,
                ayahInSurah = numInSurah
            )
            ayahs.add(
                MushafAyahItem(
                    globalNumber = aObj.getInt("number"),
                    surahNumber = surahNum,
                    surahNameAr = meta.nameAr,
                    surahNameEn = meta.nameEn,
                    numberInSurah = numInSurah,
                    textUthmani = cleanText,
                    pageNumber = page,
                    juzNumber = aObj.optInt("juz", QuranCatalog.juzForPage(page)),
                    hizbQuarter = aObj.optInt("hizbQuarter", 1),
                    sajdah = parseSajdaField(aObj.opt("sajda")),
                    words = emptyList()
                )
            )
        }
        val first = ayahs.firstOrNull()
        return MushafPageModel(
            pageNumber = page,
            juzNumber = first?.juzNumber ?: QuranCatalog.juzForPage(page),
            hizbQuarter = first?.hizbQuarter ?: 1,
            ayahs = ayahs
        )
    }

    private fun searchOnlineAlQuranCloud(query: String): List<QuranSearchHit> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "${quranFallbackApiBase}/search/$encoded/all/quran-simple-clean"
        val req = Request.Builder().url(url).build()
        val body = httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            resp.body?.string() ?: return emptyList()
        }
        val data = JSONObject(body).optJSONObject("data") ?: return emptyList()
        val matches = data.optJSONArray("matches") ?: return emptyList()
        val hits = ArrayList<QuranSearchHit>(matches.length().coerceAtMost(40))
        for (i in 0 until matches.length().coerceAtMost(40)) {
            val m = matches.getJSONObject(i)
            val sObj = m.getJSONObject("surah")
            val surahNum = sObj.getInt("number")
            val meta = QuranCatalog.surahByNumber(surahNum)
            val ayahNum = m.getInt("numberInSurah")
            val globalNum = m.optInt("number", QuranCatalog.globalAyahNumber(surahNum, ayahNum))
            val approxPage = estimatePageForAyah(surahNum, ayahNum, meta)
            hits.add(
                QuranSearchHit(
                    globalAyahNumber = globalNum,
                    surahNumber = surahNum,
                    ayahNumber = ayahNum,
                    pageNumber = approxPage,
                    surahNameAr = meta.nameAr,
                    surahNameEn = meta.nameEn,
                    uthmaniText = m.getString("text")
                )
            )
        }
        return hits
    }

    private fun estimatePageForAyah(surahNum: Int, ayahNum: Int, meta: com.example.domain.SurahMeta): Int {
        val nextStart = if (surahNum < 114) QuranCatalog.surahByNumber(surahNum + 1).startPage else 604
        val span = (nextStart - meta.startPage).coerceAtLeast(1)
        val fraction = ((ayahNum - 1).toFloat() / meta.ayahCount.coerceAtLeast(1))
        return (meta.startPage + (fraction * span).toInt()).coerceIn(meta.startPage, nextStart)
    }

    private fun cleanPrependedBismillah(raw: String, surahNumber: Int, ayahInSurah: Int): String {
        if (surahNumber == 1 || surahNumber == 9 || ayahInSurah != 1) return raw.trim()
        val trimmed = raw.trim()
        // AlQuranCloud often prefixes "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ " or similar before Ayah 1
        val normalizedPrefix = ArabicTextNormalizer.normalize("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ")
        val words = trimmed.split(Regex("\\s+"))
        if (words.size > 4) {
            val firstFourNorm = ArabicTextNormalizer.normalize(words.take(4).joinToString(" "))
            if (firstFourNorm == normalizedPrefix) {
                return words.drop(4).joinToString(" ").trim()
            }
        }
        return trimmed
    }

    private fun parseSajdaField(raw: Any?): Boolean = when (raw) {
        is Boolean -> raw
        is JSONObject -> raw.optBoolean("recommended", false) || raw.optBoolean("obligatory", false)
        else -> false
    }

    private fun MushafAyahItem.toIndexEntity() = QuranAyahIndexEntity(
        globalAyahNumber = globalNumber,
        surahNumber = surahNumber,
        ayahNumber = numberInSurah,
        pageNumber = pageNumber,
        juzNumber = juzNumber,
        hizbQuarter = hizbQuarter,
        surahNameAr = surahNameAr,
        surahNameEn = surahNameEn,
        uthmaniText = textUthmani,
        normalizedText = ArabicTextNormalizer.normalize(textUthmani),
        sajdah = sajdah
    )

    private fun modelToEntity(model: MushafPageModel): MushafPageEntity {
        val arr = JSONArray()
        for (a in model.ayahs) {
            val obj = JSONObject().apply {
                put("globalNumber", a.globalNumber)
                put("surahNumber", a.surahNumber)
                put("surahNameAr", a.surahNameAr)
                put("surahNameEn", a.surahNameEn)
                put("numberInSurah", a.numberInSurah)
                put("textUthmani", a.textUthmani)
                put("pageNumber", a.pageNumber)
                put("juzNumber", a.juzNumber)
                put("hizbQuarter", a.hizbQuarter)
                put("sajdah", a.sajdah)
                val wArr = JSONArray()
                for (w in a.words) {
                    wArr.put(JSONObject().apply {
                        put("textUthmani", w.textUthmani)
                        put("lineNumber", w.lineNumber)
                        put("isEndOfAyah", w.isEndOfAyah)
                    })
                }
                put("words", wArr)
            }
            arr.put(obj)
        }
        return MushafPageEntity(
            pageNumber = model.pageNumber,
            juzNumber = model.juzNumber,
            hizbQuarter = model.hizbQuarter,
            ayahsJson = arr.toString()
        )
    }

    private fun entityToModel(entity: MushafPageEntity): MushafPageModel {
        val arr = JSONArray(entity.ayahsJson)
        val ayahs = ArrayList<MushafAyahItem>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val wArr = obj.optJSONArray("words")
            val words = ArrayList<MushafWordToken>()
            if (wArr != null) {
                for (wIdx in 0 until wArr.length()) {
                    val wObj = wArr.getJSONObject(wIdx)
                    words.add(
                        MushafWordToken(
                            textUthmani = wObj.getString("textUthmani"),
                            lineNumber = wObj.getInt("lineNumber"),
                            isEndOfAyah = wObj.optBoolean("isEndOfAyah", false)
                        )
                    )
                }
            }
            ayahs.add(
                MushafAyahItem(
                    globalNumber = obj.getInt("globalNumber"),
                    surahNumber = obj.getInt("surahNumber"),
                    surahNameAr = obj.getString("surahNameAr"),
                    surahNameEn = obj.getString("surahNameEn"),
                    numberInSurah = obj.getInt("numberInSurah"),
                    textUthmani = obj.getString("textUthmani"),
                    pageNumber = obj.getInt("pageNumber"),
                    juzNumber = obj.getInt("juzNumber"),
                    hizbQuarter = obj.getInt("hizbQuarter"),
                    sajdah = obj.optBoolean("sajdah", false),
                    words = words
                )
            )
        }
        return MushafPageModel(
            pageNumber = entity.pageNumber,
            juzNumber = entity.juzNumber,
            hizbQuarter = entity.hizbQuarter,
            ayahs = ayahs
        )
    }

    private companion object {
        const val TAG = "MushafRepository"
    }
}
