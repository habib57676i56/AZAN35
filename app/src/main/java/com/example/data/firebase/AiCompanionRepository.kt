package com.example.data.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
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

class AiCompanionRepository {

    private val companionModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val khutbahSummaryModel by lazy {
        val schema = Schema.obj(
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
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    fun askScholarlyCompanionStream(question: String, languageName: String): Flow<String> = flow {
        val prompt = """
            You are a respectful, authentic Islamic knowledge assistant inside the Mihrab app.
            Respond in $languageName.
            Guidelines:
            1. Ground every answer in the Quran and authentic Sunnah (citing Surah:Ayah or Hadith collection such as Sahih al-Bukhari / Sahih Muslim).
            2. Present mainstream scholarly consensus clearly and gently.
            3. End with a brief reminder: for personal legal rulings (Fatwa), consult a qualified local scholar or Dar al-Ifta.
            4. Keep the tone warm, concise, and spiritually uplifting.

            User question: $question
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            companionModel.generateContentStream(prompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("AI companion returned an empty response.")
        }
    }

    suspend fun evaluateRecitation(
        surahName: String,
        ayahRef: String,
        expectedArabic: String,
        recitedText: String
    ): Result<String> {
        return try {
            val prompt = """
                You are Al-Musammi', a gentle Quran memorization coach.
                The user recited Surah $surahName ($ayahRef).
                Expected Arabic verse: $expectedArabic
                User's speech recognition / typed attempt: $recitedText

                Provide:
                - Accuracy encouragement and any missed or substituted words.
                - One practical Tajweed / Waqf tip for this specific verse.
                - Keep it concise (4-5 bullet points max).
            """.trimIndent()
            val response = withTimeout(60_000L) {
                companionModel.generateContent(prompt)
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

    suspend fun generateKhutbahOrWirdSummary(notesOrTopic: String): Result<KhutbahSummaryResult> {
        return try {
            val prompt = """
                Create a structured Islamic Khutbah / Daily Wird reflection summary in English based on:
                "$notesOrTopic"
                Keep takeaways grounded in Quran and authentic Sunnah.
            """.trimIndent()
            val response = withTimeout(60_000L) {
                khutbahSummaryModel.generateContent(prompt)
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
