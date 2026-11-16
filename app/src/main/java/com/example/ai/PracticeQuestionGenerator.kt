package com.example.ai

import android.util.Log
import com.example.domain.model.Chapter
import com.example.domain.model.PracticeDifficulty
import com.example.domain.model.PracticeDifficultyFilter
import com.example.domain.model.PracticeQuestion
import com.example.domain.model.PracticeQuestionType
import com.example.domain.model.Topic
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

class PracticeQuestionGenerator {

    private val questionSchema = Schema.obj(
        properties = mapOf(
            "type" to Schema.enumeration(
                values = listOf("MCQ", "NUMERICAL", "SHORT_ANSWER"),
                description = "Question format: MCQ, NUMERICAL, or SHORT_ANSWER"
            ),
            "difficulty" to Schema.enumeration(
                values = listOf("EASY", "MEDIUM", "HARD"),
                description = "JEE difficulty tier"
            ),
            "conceptTag" to Schema.string(
                description = "Core JEE sub-concept tested by this question"
            ),
            "prompt" to Schema.string(
                description = "Clear JEE problem statement using readable math notation"
            ),
            "options" to Schema.array(
                items = Schema.string(),
                description = "Exactly 4 distinct options for MCQ questions, or an empty array for NUMERICAL and SHORT_ANSWER"
            ),
            "correctAnswer" to Schema.string(
                description = "For MCQ: exact matching string from options. For NUMERICAL: clean decimal/integer value without units. For SHORT_ANSWER: concise canonical answer."
            ),
            "numericalTolerance" to Schema.double(
                description = "Allowed numerical tolerance (e.g. 0.05) for NUMERICAL questions, or 0.0 for others"
            ),
            "solutionSteps" to Schema.string(
                description = "Concise step-by-step JEE solution explaining the key formula or theorem"
            )
        ),
        optionalProperties = listOf("numericalTolerance")
    )

    private val responseSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = questionSchema,
                description = "Generated JEE practice questions matching requested count and types"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = this@PracticeQuestionGenerator.responseSchema
                temperature = 0.65f
            }
        )
    }

    suspend fun generateQuestions(
        chapter: Chapter,
        topic: Topic?,
        questionCount: Int,
        difficultyFilter: PracticeDifficultyFilter,
        allowedTypes: Set<PracticeQuestionType>
    ): List<PracticeQuestion> {
        val safeCount = questionCount.coerceIn(1, 20)
        val effectiveTypes = allowedTypes.ifEmpty { PracticeQuestionType.entries.toSet() }
        val typesListText = effectiveTypes.joinToString(", ") { it.name }
        val targetTopicText = topic?.name ?: "Full Chapter Overview (core topics across the chapter)"
        val contextNotes = buildString {
            if (chapter.importantFormulas.isNotBlank()) {
                append("\nStudent formula notes: ${chapter.importantFormulas.take(400)}")
            }
            if (topic != null && topic.notes.isNotBlank()) {
                append("\nTopic notes: ${topic.notes.take(300)}")
            }
        }

        val difficultyInstruction = when (difficultyFilter) {
            PracticeDifficultyFilter.MIXED ->
                "Vary difficulty across EASY (conceptual warm-up), MEDIUM (JEE Main level), and HARD (JEE Advanced multi-concept level)."
            PracticeDifficultyFilter.EASY ->
                "Focus primarily on EASY foundational JEE questions, with 1-2 MEDIUM checks."
            PracticeDifficultyFilter.MEDIUM ->
                "Focus on MEDIUM JEE Main standard analytical & numerical questions."
            PracticeDifficultyFilter.HARD ->
                "Focus on HARD JEE Advanced multi-concept and deep reasoning questions."
        }

        val prompt = """
            You are an expert JEE Main & JEE Advanced question author for Class 11 & 12 aspirants.
            Generate exactly $safeCount high-quality practice questions tailored to:
            - Subject: ${chapter.subjectEnum.label}
            - Class Level: ${chapter.classEnum.label}
            - Chapter: ${chapter.name}
            - Topic: $targetTopicText$contextNotes

            Requirements:
            1. Allowed question types: $typesListText. Distribute thoughtfully across the allowed types based on core concepts of this topic.
            2. $difficultyInstruction
            3. For MCQ questions: provide exactly 4 distinct options in "options", and set "correctAnswer" to the exact string of the single correct option.
            4. For NUMERICAL questions: set "options" to [] (empty array), and set "correctAnswer" to a plain number string (for example "4.5" or "12") with appropriate units noted in the prompt.
            5. For SHORT_ANSWER questions: set "options" to [] (empty array), and set "correctAnswer" to a concise canonical statement, formula, or key phrase.
            6. Include a clear "conceptTag" and step-by-step "solutionSteps" for every question.
            7. Use clean readable Unicode math symbols (×, ÷, √, π, Δ, θ, α, β, ω, ≤, ≥, ², ³, →) instead of raw LaTeX backslashes where possible.
        """.trimIndent()

        return try {
            val response = withTimeout(TIMEOUT_MS) {
                model.generateContent(prompt)
            }
            val jsonText = response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("AI returned an empty question set.")

            parseQuestionsJson(jsonText, safeCount)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Practice question generation timed out", e)
            throw IllegalStateException(
                "Question generation timed out after 60 seconds. Please check your connection or request fewer questions.",
                e
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate practice questions", e)
            throw IllegalStateException(
                e.localizedMessage ?: "Failed to generate practice questions. Please try again.",
                e
            )
        }
    }

    private fun parseQuestionsJson(jsonText: String, expectedMax: Int): List<PracticeQuestion> {
        val root = JSONObject(jsonText)
        val arr = root.getJSONArray("questions")
        val list = mutableListOf<PracticeQuestion>()
        for (i in 0 until arr.length().coerceAtMost(expectedMax)) {
            val obj = arr.getJSONObject(i)
            val type = PracticeQuestionType.fromString(obj.optString("type", "MCQ"))
            val difficulty = PracticeDifficulty.fromString(obj.optString("difficulty", "MEDIUM"))
            val optionsJson = obj.optJSONArray("options")
            val options = if (optionsJson != null) {
                (0 until optionsJson.length()).mapNotNull { idx ->
                    optionsJson.optString(idx)?.takeIf { it.isNotBlank() }
                }
            } else emptyList()

            val promptText = obj.optString("prompt").trim()
            val correctAnswer = obj.optString("correctAnswer").trim()
            if (promptText.isBlank() || correctAnswer.isBlank()) continue

            val finalType = if (type == PracticeQuestionType.MCQ && options.size < 2) {
                PracticeQuestionType.SHORT_ANSWER
            } else {
                type
            }

            list.add(
                PracticeQuestion(
                    id = "pq_${i + 1}_${UUID.randomUUID().toString().take(6)}",
                    type = finalType,
                    difficulty = difficulty,
                    conceptTag = obj.optString("conceptTag").ifBlank { "Core Concept" },
                    prompt = promptText,
                    options = if (finalType == PracticeQuestionType.MCQ) options else emptyList(),
                    correctAnswer = correctAnswer,
                    numericalTolerance = obj.optDouble("numericalTolerance", 0.02).let {
                        if (it.isNaN() || it <= 0.0) 0.02 else it
                    },
                    solutionSteps = obj.optString("solutionSteps").ifBlank { "Review core derivation for this concept." }
                )
            )
        }
        if (list.isEmpty()) {
            throw IllegalStateException("No valid practice questions could be parsed from the AI response.")
        }
        return list
    }

    private companion object {
        const val TAG = "PracticeQuestionGen"
        const val MODEL_NAME = "gemini-3.8-flash"
        const val TIMEOUT_MS = 60_000L
    }
}
