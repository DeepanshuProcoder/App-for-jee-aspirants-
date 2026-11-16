package com.example.domain.model

enum class PracticeQuestionType(val label: String) {
    MCQ("MCQ"),
    NUMERICAL("Numerical"),
    SHORT_ANSWER("Short Answer");

    companion object {
        fun fromString(raw: String): PracticeQuestionType =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: MCQ
    }
}

enum class PracticeDifficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard");

    companion object {
        fun fromString(raw: String): PracticeDifficulty =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: MEDIUM
    }
}

enum class PracticeDifficultyFilter(val label: String) {
    MIXED("Mixed Difficulty"),
    EASY("Easy Focus"),
    MEDIUM("JEE Main (Medium)"),
    HARD("JEE Adv (Hard)")
}

data class PracticeQuestion(
    val id: String,
    val type: PracticeQuestionType,
    val difficulty: PracticeDifficulty,
    val conceptTag: String,
    val prompt: String,
    val options: List<String> = emptyList(),
    val correctAnswer: String,
    val numericalTolerance: Double = 0.02,
    val solutionSteps: String
)

data class EvaluatedQuestionResult(
    val question: PracticeQuestion,
    val userAnswer: String,
    val isCorrect: Boolean,
    val feedback: String
)

data class ChapterTestScheduleImpact(
    val scheduleResult: ScheduleComputationResult,
    val triggeredEarlierRevision: Boolean,
    val weakConceptTags: List<String>,
    val testChapterResult: TestChapterResult,
    val accuracyPercent: Double
)

data class PracticeTestOutcomeSummary(
    val chapterId: String,
    val chapterName: String,
    val topicId: String?,
    val topicName: String?,
    val totalQuestions: Int,
    val correctCount: Int,
    val accuracyPercent: Double,
    val durationMinutes: Int,
    val triggeredEarlierRevision: Boolean,
    val nextRevisionDate: String,
    val rescheduleExplanation: String,
    val weakConceptTags: List<String>,
    val questionResults: List<EvaluatedQuestionResult>
)

sealed interface PracticeTestUiState {
    data object Idle : PracticeTestUiState

    data class Generating(
        val chapterName: String,
        val topicName: String?,
        val requestedCount: Int
    ) : PracticeTestUiState

    data class InProgress(
        val chapter: Chapter,
        val topic: Topic?,
        val questions: List<PracticeQuestion>,
        val currentIndex: Int = 0,
        val userAnswers: Map<String, String> = emptyMap(),
        val elapsedSeconds: Int = 0
    ) : PracticeTestUiState

    data class Completed(
        val summary: PracticeTestOutcomeSummary
    ) : PracticeTestUiState

    data class Error(
        val message: String
    ) : PracticeTestUiState
}
