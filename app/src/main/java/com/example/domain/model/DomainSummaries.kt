package com.example.domain.model

enum class QueueCategory(val label: String) {
    OVERDUE("Overdue"),
    DUE_TODAY("Due Today"),
    URGENT_WEAK("Weak Topic Review"),
    TEST_PREP("Test Preparation"),
    UPCOMING("Upcoming Preview")
}

data class RevisionQueueItem(
    val key: String,
    val subject: Subject,
    val chapterId: String,
    val chapterName: String,
    val topicId: String?,
    val topicName: String?,
    val category: QueueCategory,
    val dueDate: String,
    val originalDueDate: String,
    val overdueDays: Int,
    val estimatedMinutes: Int,
    val estimatedRetention: Double,
    val importance: Int,
    val currentIntervalDays: Int,
    val status: ChapterStatus,
    val priorityScore: Double,
    val subtitleReason: String
)

data class SubjectProgressSummary(
    val subject: Subject,
    val totalChapters: Int,
    val studiedChapters: Int,
    val strongChapters: Int,
    val needsAttentionChapters: Int,
    val dueOrOverdueCount: Int,
    val averageEstimatedRetention: Double?,
    val averageAccuracy: Double?
)

data class AlertItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val severity: AlertSeverity,
    val chapterId: String? = null
)

enum class AlertSeverity {
    HIGH, MEDIUM, INFO
}

data class DashboardMetrics(
    val dueTodayCount: Int,
    val completedTodayCount: Int,
    val remainingTodayCount: Int,
    val overdueCount: Int,
    val upcoming7DaysCount: Int,
    val estimatedStudyMinutes: Int,
    val weeklyCompletionRate: Double?,
    val monthlyCompletionRate: Double?,
    val revisionStreakDays: Int,
    val strongChaptersCount: Int,
    val weakChaptersCount: Int,
    val overallEstimatedRetention: Double?,
    val subjectSummaries: List<SubjectProgressSummary>,
    val alerts: List<AlertItem>,
    val retentionTrendPoints: List<Pair<String, Double>>
)

data class ScheduleComputationResult(
    val nextIntervalDays: Int,
    val nextRevisionDate: String,
    val originalTargetDate: String,
    val wasLoadBalancedOrShifted: Boolean,
    val newConsecutiveSuccesses: Int,
    val newStatus: ChapterStatus,
    val estimatedRetentionAfter: Double,
    val explanation: String,
    val recommendedPracticeQuestions: Int = 0,
    val recommendedAction: String = ""
)

data class RecommendationItem(
    val id: String,
    val subject: Subject,
    val chapterId: String,
    val topicId: String?,
    val title: String,
    val message: String,
    val actionLabel: String
)

data class WeeklyReport(
    val weekStartDate: String,
    val weekEndDate: String,
    val revisionsScheduled: Int,
    val revisionsCompleted: Int,
    val revisionsMissed: Int,
    val subjectCompletionMap: Map<Subject, Int>,
    val strongChapters: List<String>,
    val weakChapters: List<String>,
    val repeatedErrorsCount: Int,
    val averageAccuracy: Double?,
    val nextWeekWorkloadCount: Int,
    val suggestedActionPlan: List<String>
)

data class MonthlyReport(
    val monthLabel: String,
    val totalSessions: Int,
    val totalQuestionsSolved: Long,
    val averageAccuracy: Double?,
    val strongChaptersCount: Int,
    val activeChaptersCount: Int,
    val subjectRetentionMap: Map<Subject, Double?>,
    val topMistakeCategory: ErrorCategory?
)
