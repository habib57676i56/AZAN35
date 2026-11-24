package com.example.data.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class KhutbahSummaryResult(
    val title: String,
    val keyPoints: List<String>,
    val weeklyAction: String,
    val reflectionVerse: String
)

data class HifzQuizItem(
    val promptVerse: String,
    val options: List<String>,
    val correctIndex: Int,
    val surahRef: String,
    val mutashabihatAlert: String,
    val explanation: String
)

class AiCompanionRepository {

    private var cachedKey: Pair<String, String>? = null
    private var cachedChatModel: GenerativeModel? = null

    private fun resolveSafeModelName(requested: String): String {
        val trimmed = requested.trim()
        return if (trimmed.startsWith("gemini-3") || trimmed.startsWith("gemini-4")) {
            trimmed
        } else {
            "gemini-3.8-flash"
        }
    }

    private fun getChatModel(config: AiRuntimeConfig): GenerativeModel {
        val safeModel = resolveSafeModelName(config.modelName)
        val sysPrompt = config.systemPrompt.ifBlank { AiRuntimeConfig.DEFAULT_ISLAMIC_SYSTEM_PROMPT }
        val key = safeModel to sysPrompt
        if (cachedKey == key && cachedChatModel != null) {
            return cachedChatModel!!
        }
        val created = Firebase.ai.generativeModel(
            modelName = safeModel,
            systemInstruction = content { text(sysPrompt) }
        )
        cachedKey = key
        cachedChatModel = created
        return created
    }

    private fun getStructuredModel(config: AiRuntimeConfig, schema: Schema): GenerativeModel {
        val safeModel = resolveSafeModelName(config.modelName)
        val sysPrompt = config.systemPrompt.ifBlank { AiRuntimeConfig.DEFAULT_ISLAMIC_SYSTEM_PROMPT }
        return Firebase.ai.generativeModel(
            modelName = safeModel,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            },
            systemInstruction = content { text(sysPrompt) }
        )
    }

    private val khutbahSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string("Concise title of the Khutbah or reflection"),
            "keyPoints" to Schema.array(
                items = Schema.string(),
                description = "3 to 4 actionable spiritual takeaways"
            ),
            "weeklyAction" to Schema.string("One practical deed for the week"),
            "reflectionVerse" to Schema.string("A relevant Quranic ayah or authentic Hadith reference")
        )
    )

    private val hifzQuizSchema = Schema.obj(
        properties = mapOf(
            "promptVerse" to Schema.string("An incomplete Arabic Quranic verse ending with '...' for the user to complete"),
            "options" to Schema.array(
                items = Schema.string(),
                description = "4 plausible Arabic verse continuations (1 exact continuation and 3 Mutashabihat or subtle distractors)"
            ),
            "correctIndex" to Schema.integer("Zero-based index (0..3) of the exact continuation"),
            "surahRef" to Schema.string("Surah name and Ayah reference e.g. البقرة ٢:١٥٣"),
            "mutashabihatAlert" to Schema.string("Alert highlighting similar verses (المتشابهات) in other Surahs and how to tell them apart"),
            "explanation" to Schema.string("Brief concise explanation and memorization anchor")
        )
    )

    fun askAssistantChatStream(
        history: List<AiChatDoc>,
        question: String,
        config: AiRuntimeConfig,
        languageName: String
    ): Flow<String> = flow {
        if (!config.enabled) {
            throw IllegalStateException("AI Assistant is disabled by Admin configuration.")
        }
        val model = getChatModel(config)
        val priorHistory = history
            .filter { it.content.isNotBlank() }
            .takeLast(12)
            .map { doc ->
                val geminiRole = if (doc.role == "assistant") "model" else "user"
                content(role = geminiRole) { text(doc.content) }
            }

        val chat = model.startChat(history = priorHistory)
        val userTurn = """
            أجب بلغة السؤال المفضلة ($languageName) وبأسلوب واضح وموثق من القرآن الكريم والسنة الصحيحة وأقوال أئمة التفسير:
            $question
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            chat.sendMessageStream(userTurn).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("AI assistant returned an empty response.")
        }
    }

    fun askAyahTafsirStream(
        surahNameAr: String,
        surahNumber: Int,
        ayahNumber: Int,
        arabicText: String,
        config: AiRuntimeConfig,
        languageName: String
    ): Flow<String> = flow {
        if (!config.enabled) {
            throw IllegalStateException("AI Assistant is disabled by Admin configuration.")
        }
        val model = getChatModel(config)
        val prompt = """
            المستخدم يقرأ في مصحف المدينة وطلب التفسير والتدبر لهذه الآية الكريمة.
            أجب بلغة ($languageName) وبوضوح:
            السورة: $surahNameAr ($surahNumber:$ayahNumber)
            النص: $arabicText

            قدّم في نقاط منظمة:
            ١. المعنى الإجمالي الميسر (مستمداً من التفسير الميسر وابن كثير والسعدي).
            ٢. معاني المفردات الغريبة أو الدقيقة في الآية.
            ٣. لطيفة تدبرية أو عمل إيماني تطبيقي من الآية.
            ٤. تنبيه موجز إن كان في الآية موضع متشابهات شهير في القرآن.
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            model.generateContentStream(prompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("AI Tafsir returned an empty response.")
        }
    }

    fun askScholarlyCompanionStream(question: String, languageName: String): Flow<String> =
        askAssistantChatStream(
            history = emptyList(),
            question = question,
            config = AiRuntimeConfig(),
            languageName = languageName
        )

    suspend fun evaluateRecitation(
        surahName: String,
        ayahRef: String,
        expectedArabic: String,
        recitedText: String,
        config: AiRuntimeConfig = AiRuntimeConfig()
    ): Result<String> {
        if (!config.enabled) {
            return Result.failure(IllegalStateException("AI Assistant is currently disabled."))
        }
        return try {
            val model = getChatModel(config)
            val prompt = """
                أنت المُسمِّع، مدرب حفظ القرآن الكريم اللطيف.
                المستخدم قرأ من سورة $surahName ($ayahRef).
                الآية الصحيحة: $expectedArabic
                محاولة المستخدم: $recitedText

                قدّم تقييماً عملياً بالعربية:
                - نسبة الإتقان والكلمات التي سقطت أو تبدلت إن وجدت.
                - نصيحة تجويد أو وقف وابتداء عملية لهذه الآية.
                - تشجيع لطيف للمراجعة.
            """.trimIndent()
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val text = response.text
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("Memorization coach returned an empty response."))
            } else {
                Result.success(text)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI evaluation timed out", e)
            Result.failure(Exception("Evaluation timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI evaluation failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateMemorizationQuiz(
        topicOrSurah: String,
        config: AiRuntimeConfig = AiRuntimeConfig(),
        languageName: String = "العربية"
    ): Result<HifzQuizItem> {
        if (!config.enabled) {
            return Result.failure(IllegalStateException("AI Assistant is currently disabled."))
        }
        return try {
            val model = getStructuredModel(config, hifzQuizSchema)
            val scopeHint = topicOrSurah.trim().ifBlank { "جزء عمّ أو السور المألوفة مثل الكهف والملك ويس والبقرة" }
            val prompt = """
                أنشئ سؤال اختبار حفظ قرآني تفاعلي (أكمل الآية مع تنبيه المتشابهات) في: "$scopeHint".
                اكتب المتن القرآني باللغة العربية دائماً مع ضبطه، واكتب التوضيح بلغة ($languageName).
                الشروط:
                - promptVerse: بداية آية واضحة تنتهي بـ ... ليكملها الحافظ.
                - options: ٤ خيارات عربية متقاربة (واحد صحيح والبقية من المتشابهات القريبة).
                - correctIndex: رقم الخيار الصحيح (من 0 إلى 3).
                - mutashabihatAlert: تنبيه دقيق للآيات المتشابهة في سور أخرى وكيفية التفريق بينها.
                - explanation: قاعدة ربط للحفظ.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty quiz response.")
            val json = JSONObject(rawJson)
            val optsJson = json.getJSONArray("options")
            val options = (0 until optsJson.length()).map { optsJson.getString(it) }
            require(options.size >= 2) { "Insufficient quiz options generated." }
            val safeIdx = json.optInt("correctIndex", 0).coerceIn(0, options.lastIndex)
            Result.success(
                HifzQuizItem(
                    promptVerse = json.getString("promptVerse"),
                    options = options,
                    correctIndex = safeIdx,
                    surahRef = json.optString("surahRef", ""),
                    mutashabihatAlert = json.optString("mutashabihatAlert", ""),
                    explanation = json.optString("explanation", "")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Hifz quiz timed out", e)
            Result.failure(Exception("Quiz generation timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Hifz quiz failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateKhutbahOrWirdSummary(
        notesOrTopic: String,
        config: AiRuntimeConfig = AiRuntimeConfig(),
        languageName: String = "العربية"
    ): Result<KhutbahSummaryResult> {
        if (!config.enabled) {
            return Result.failure(IllegalStateException("AI Assistant is currently disabled."))
        }
        return try {
            val model = getStructuredModel(config, khutbahSchema)
            val prompt = """
                أنشئ ملخصاً عملياً منظماً لخطبة الجمعة أو الورد اليومي بلغة ($languageName) بناءً على:
                "$notesOrTopic"
                استند إلى القرآن الكريم والسنة الصحيحة.
            """.trimIndent()
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty structured response.")
            val json = JSONObject(rawJson)
            val pointsJson = json.getJSONArray("keyPoints")
            val points = (0 until pointsJson.length()).map { pointsJson.getString(it) }
            Result.success(
                KhutbahSummaryResult(
                    title = json.getString("title"),
                    keyPoints = points,
                    weeklyAction = json.getString("weeklyAction"),
                    reflectionVerse = json.getString("reflectionVerse")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Khutbah summary timed out", e)
            Result.failure(Exception("Summary timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Khutbah summary failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "AiCompanionRepo"
    }
}
