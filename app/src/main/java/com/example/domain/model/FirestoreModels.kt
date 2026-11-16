package com.example.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val classLevel: String = ClassLevel.CLASS_11.name,
    val syllabusVersion: String = "JEE-2026-v1",
    val defaultIntervals: List<Long> = listOf(1L, 3L, 7L, 14L, 30L, 60L, 90L),
    val dailyRevisionCapacity: Long = 6L,
    val preferredStudyTime: String = "18:30",
    val darkModePreference: String = ThemeMode.DARK.name,
    val notificationsEnabled: Boolean = false,
    val notifyTodayQueue: Boolean = true,
    val notifyOverdue: Boolean = true,
    val notifyTests: Boolean = true,
    val notifyWeakTopics: Boolean = true,
    val notifyWeeklyReport: Boolean = true,
    val lastNotifiedDate: String? = null,
    val blockedDates: List<String> = emptyList(),
    val seededInitialCatalog: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun intervalsAsInts(): List<Int> =
        defaultIntervals.mapNotNull { it.toInt().takeIf { d -> d >= 1 } }.ifEmpty { listOf(1, 3, 7, 14, 30, 60, 90) }

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName.take(120),
        "email" to email?.take(200),
        "classLevel" to classLevel,
        "syllabusVersion" to syllabusVersion.take(40),
        "defaultIntervals" to defaultIntervals,
        "dailyRevisionCapacity" to dailyRevisionCapacity.coerceIn(1L, 30L),
        "preferredStudyTime" to preferredStudyTime.take(16),
        "darkModePreference" to darkModePreference,
        "notificationsEnabled" to notificationsEnabled,
        "notifyTodayQueue" to notifyTodayQueue,
        "notifyOverdue" to notifyOverdue,
        "notifyTests" to notifyTests,
        "notifyWeakTopics" to notifyWeakTopics,
        "notifyWeeklyReport" to notifyWeeklyReport,
        "lastNotifiedDate" to lastNotifiedDate,
        "blockedDates" to blockedDates.take(120),
        "seededInitialCatalog" to seededInitialCatalog,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@IgnoreExtraProperties
data class Chapter(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val subject: String = Subject.PHYSICS.name,
    val section: String = "",
    val classLevel: String = ClassLevel.CLASS_11.name,
    val orderIndex: Long = 0L,
    val isArchived: Boolean = false,
    val syllabusVersion: String = "JEE-2026-v1",
    val status: String = ChapterStatus.NOT_STARTED.name,
    val initialLearningDate: String = "",
    val completedRevisions: Long = 0L,
    val lastRevisionDate: String = "",
    val nextRevisionDate: String = "",
    val originalDueDate: String = "",
    val currentIntervalDays: Long = 0L,
    val consecutiveSuccesses: Long = 0L,
    val understandingRating: Double = 0.0,
    val problemSolvingRating: Double = 0.0,
    val confidenceRating: Double = 0.0,
    val difficultyRating: Double = 0.0,
    val totalQuestions: Long = 0L,
    val correctQuestions: Long = 0L,
    val incorrectQuestions: Long = 0L,
    val unsolvedQuestions: Long = 0L,
    val estimatedRetention: Double = 0.0,
    val importance: Long = 3L,
    val lastRescheduleReason: String = "",
    val personalNotes: String = "",
    val importantFormulas: String = "",
    val commonMistakes: String = "",
    val difficultQuestions: String = "",
    val skipReason: String = "",
    val skippedUntilDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val subjectEnum: Subject get() = Subject.fromString(subject)
    val sectionEnum: ChemistrySection get() = ChemistrySection.fromString(section)
    val classEnum: ClassLevel get() = ClassLevel.fromString(classLevel)
    val statusEnum: ChapterStatus get() = ChapterStatus.fromString(status)

    val accuracyPercent: Double
        get() = if (totalQuestions > 0L) (correctQuestions * 100.0) / totalQuestions else 0.0

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "name" to name.take(120),
        "subject" to subject,
        "section" to section,
        "classLevel" to classLevel,
        "orderIndex" to orderIndex,
        "isArchived" to isArchived,
        "syllabusVersion" to syllabusVersion.take(40),
        "status" to status,
        "initialLearningDate" to initialLearningDate.take(32),
        "completedRevisions" to completedRevisions,
        "lastRevisionDate" to lastRevisionDate.take(32),
        "nextRevisionDate" to nextRevisionDate.take(32),
        "originalDueDate" to originalDueDate.take(32),
        "currentIntervalDays" to currentIntervalDays,
        "consecutiveSuccesses" to consecutiveSuccesses,
        "understandingRating" to understandingRating.coerceIn(0.0, 5.0),
        "problemSolvingRating" to problemSolvingRating.coerceIn(0.0, 5.0),
        "confidenceRating" to confidenceRating.coerceIn(0.0, 5.0),
        "difficultyRating" to difficultyRating.coerceIn(0.0, 5.0),
        "totalQuestions" to totalQuestions,
        "correctQuestions" to correctQuestions,
        "incorrectQuestions" to incorrectQuestions,
        "unsolvedQuestions" to unsolvedQuestions,
        "estimatedRetention" to estimatedRetention.coerceIn(0.0, 100.0),
        "importance" to importance.coerceIn(1L, 5L),
        "lastRescheduleReason" to lastRescheduleReason.take(500),
        "personalNotes" to personalNotes.take(4000),
        "importantFormulas" to importantFormulas.take(4000),
        "commonMistakes" to commonMistakes.take(4000),
        "difficultQuestions" to difficultQuestions.take(4000),
        "skipReason" to skipReason.take(300),
        "skippedUntilDate" to skippedUntilDate.take(32),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class Topic(
    @DocumentId val id: String = "",
    val userId: String = "",
    val chapterId: String = "",
    val subject: String = Subject.PHYSICS.name,
    val name: String = "",
    val orderIndex: Long = 0L,
    val isArchived: Boolean = false,
    val status: String = ChapterStatus.NOT_STARTED.name,
    val initialLearningDate: String = "",
    val completedRevisions: Long = 0L,
    val lastRevisionDate: String = "",
    val nextRevisionDate: String = "",
    val originalDueDate: String = "",
    val currentIntervalDays: Long = 0L,
    val consecutiveSuccesses: Long = 0L,
    val understandingRating: Double = 0.0,
    val confidenceRating: Double = 0.0,
    val difficultyRating: Double = 0.0,
    val totalQuestions: Long = 0L,
    val correctQuestions: Long = 0L,
    val incorrectQuestions: Long = 0L,
    val unsolvedQuestions: Long = 0L,
    val estimatedRetention: Double = 0.0,
    val lastOutcome: String = "",
    val lastRescheduleReason: String = "",
    val notes: String = "",
    val skipReason: String = "",
    val skippedUntilDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val subjectEnum: Subject get() = Subject.fromString(subject)
    val statusEnum: ChapterStatus get() = ChapterStatus.fromString(status)
    val accuracyPercent: Double
        get() = if (totalQuestions > 0L) (correctQuestions * 100.0) / totalQuestions else 0.0

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "chapterId" to chapterId,
        "subject" to subject,
        "name" to name.take(150),
        "orderIndex" to orderIndex,
        "isArchived" to isArchived,
        "status" to status,
        "initialLearningDate" to initialLearningDate.take(32),
        "completedRevisions" to completedRevisions,
        "lastRevisionDate" to lastRevisionDate.take(32),
        "nextRevisionDate" to nextRevisionDate.take(32),
        "originalDueDate" to originalDueDate.take(32),
        "currentIntervalDays" to currentIntervalDays,
        "consecutiveSuccesses" to consecutiveSuccesses,
        "understandingRating" to understandingRating.coerceIn(0.0, 5.0),
        "confidenceRating" to confidenceRating.coerceIn(0.0, 5.0),
        "difficultyRating" to difficultyRating.coerceIn(0.0, 5.0),
        "totalQuestions" to totalQuestions,
        "correctQuestions" to correctQuestions,
        "incorrectQuestions" to incorrectQuestions,
        "unsolvedQuestions" to unsolvedQuestions,
        "estimatedRetention" to estimatedRetention.coerceIn(0.0, 100.0),
        "lastOutcome" to lastOutcome,
        "lastRescheduleReason" to lastRescheduleReason.take(500),
        "notes" to notes.take(2000),
        "skipReason" to skipReason.take(300),
        "skippedUntilDate" to skippedUntilDate.take(32),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class RevisionSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val chapterId: String = "",
    val chapterName: String = "",
    val topicId: String = "",
    val topicName: String = "",
    val subject: String = Subject.PHYSICS.name,
    val sessionDate: String = "",
    val durationMinutes: Long = 0L,
    val activeRecallNotes: String = "",
    val theoryChecklist: String = "",
    val questionsAttempted: Long = 0L,
    val questionsCorrect: Long = 0L,
    val questionsIncorrect: Long = 0L,
    val questionsWithHints: Long = 0L,
    val questionsUnsolved: Long = 0L,
    val whatForgot: String = "",
    val unclearConcept: String = "",
    val mistakeMade: String = "",
    val needsPractice: String = "",
    val understandingRating: Long = 3L,
    val confidenceRating: Long = 3L,
    val difficultyRating: Long = 3L,
    val outcome: String = RevisionOutcome.GOOD.name,
    val previousIntervalDays: Long = 0L,
    val newIntervalDays: Long = 1L,
    val nextRevisionDate: String = "",
    val rescheduleExplanation: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val subjectEnum: Subject get() = Subject.fromString(subject)
    val outcomeEnum: RevisionOutcome get() = RevisionOutcome.fromString(outcome)
    val accuracyPercent: Double
        get() = if (questionsAttempted > 0L) (questionsCorrect * 100.0) / questionsAttempted else 0.0

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "chapterId" to chapterId,
        "chapterName" to chapterName.take(120),
        "topicId" to topicId.take(128),
        "topicName" to topicName.take(150),
        "subject" to subject,
        "sessionDate" to sessionDate.take(32),
        "durationMinutes" to durationMinutes.coerceIn(0L, 600L),
        "activeRecallNotes" to activeRecallNotes.take(3000),
        "theoryChecklist" to theoryChecklist.take(500),
        "questionsAttempted" to questionsAttempted.coerceAtLeast(0L),
        "questionsCorrect" to questionsCorrect.coerceAtLeast(0L),
        "questionsIncorrect" to questionsIncorrect.coerceAtLeast(0L),
        "questionsWithHints" to questionsWithHints.coerceAtLeast(0L),
        "questionsUnsolved" to questionsUnsolved.coerceAtLeast(0L),
        "whatForgot" to whatForgot.take(1500),
        "unclearConcept" to unclearConcept.take(1500),
        "mistakeMade" to mistakeMade.take(1500),
        "needsPractice" to needsPractice.take(1500),
        "understandingRating" to understandingRating.coerceIn(1L, 5L),
        "confidenceRating" to confidenceRating.coerceIn(1L, 5L),
        "difficultyRating" to difficultyRating.coerceIn(1L, 5L),
        "outcome" to outcome,
        "previousIntervalDays" to previousIntervalDays.coerceAtLeast(0L),
        "newIntervalDays" to newIntervalDays.coerceIn(1L, 3650L),
        "nextRevisionDate" to nextRevisionDate.take(32),
        "rescheduleExplanation" to rescheduleExplanation.take(800),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class CalendarEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val eventType: String = CalendarEventType.STUDY_TARGET.name,
    val subject: String = "ALL",
    val date: String = "",
    val timeSlot: String = "18:00",
    val chapterId: String = "",
    val notes: String = "",
    val isCompleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val eventTypeEnum: CalendarEventType get() = CalendarEventType.fromString(eventType)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title.take(150),
        "eventType" to eventType,
        "subject" to subject,
        "date" to date.take(32),
        "timeSlot" to timeSlot.take(16),
        "chapterId" to chapterId.take(128),
        "notes" to notes.take(1000),
        "isCompleted" to isCompleted,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

data class TestChapterResult(
    val chapterId: String = "",
    val chapterName: String = "",
    val subject: String = Subject.PHYSICS.name,
    val accuracyPercent: Double = 0.0,
    val questionsAttempted: Long = 0L,
    val questionsCorrect: Long = 0L,
    val weakFlag: Boolean = false
) {
    fun toMap(): Map<String, Any> = mapOf(
        "chapterId" to chapterId,
        "chapterName" to chapterName,
        "subject" to subject,
        "accuracyPercent" to accuracyPercent,
        "questionsAttempted" to questionsAttempted,
        "questionsCorrect" to questionsCorrect,
        "weakFlag" to weakFlag
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): TestChapterResult = TestChapterResult(
            chapterId = map["chapterId"] as? String ?: "",
            chapterName = map["chapterName"] as? String ?: "",
            subject = map["subject"] as? String ?: Subject.PHYSICS.name,
            accuracyPercent = (map["accuracyPercent"] as? Number)?.toDouble() ?: 0.0,
            questionsAttempted = (map["questionsAttempted"] as? Number)?.toLong() ?: 0L,
            questionsCorrect = (map["questionsCorrect"] as? Number)?.toLong() ?: 0L,
            weakFlag = map["weakFlag"] as? Boolean ?: false
        )
    }
}

@IgnoreExtraProperties
data class TestRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val testName: String = "",
    val testDate: String = "",
    val testType: String = TestType.ALLEN_MINOR.name,
    val physicsScore: Double = 0.0,
    val physicsMax: Double = 100.0,
    val chemistryScore: Double = 0.0,
    val chemistryMax: Double = 100.0,
    val mathsScore: Double = 0.0,
    val mathsMax: Double = 100.0,
    val totalMarks: Double = 0.0,
    val maxMarks: Double = 300.0,
    val accuracyPercent: Double = 0.0,
    val timeManagementNotes: String = "",
    val chapterResults: List<Map<String, Any>> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val testTypeEnum: TestType get() = TestType.fromString(testType)

    fun parsedChapterResults(): List<TestChapterResult> =
        chapterResults.map { TestChapterResult.fromMap(it) }

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "testName" to testName.take(150),
        "testDate" to testDate.take(32),
        "testType" to testType,
        "physicsScore" to physicsScore,
        "physicsMax" to physicsMax.coerceAtLeast(0.0),
        "chemistryScore" to chemistryScore,
        "chemistryMax" to chemistryMax.coerceAtLeast(0.0),
        "mathsScore" to mathsScore,
        "mathsMax" to mathsMax.coerceAtLeast(0.0),
        "totalMarks" to totalMarks,
        "maxMarks" to maxMarks.coerceAtLeast(0.0),
        "accuracyPercent" to accuracyPercent.coerceIn(0.0, 100.0),
        "timeManagementNotes" to timeManagementNotes.take(1500),
        "chapterResults" to chapterResults.take(40),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class ErrorRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subject: String = Subject.PHYSICS.name,
    val chapterId: String = "",
    val chapterName: String = "",
    val topicId: String = "",
    val topicName: String = "",
    val questionRef: String = "",
    val errorCategory: String = ErrorCategory.CONCEPTUAL.name,
    val mistakeExplanation: String = "",
    val correctApproach: String = "",
    val importantLesson: String = "",
    val dateRecorded: String = "",
    val reattemptStatus: String = ReattemptStatus.PENDING.name,
    val scheduledRevisionDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val subjectEnum: Subject get() = Subject.fromString(subject)
    val categoryEnum: ErrorCategory get() = ErrorCategory.fromString(errorCategory)
    val statusEnum: ReattemptStatus get() = ReattemptStatus.fromString(reattemptStatus)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "subject" to subject,
        "chapterId" to chapterId.take(128),
        "chapterName" to chapterName.take(120),
        "topicId" to topicId.take(128),
        "topicName" to topicName.take(150),
        "questionRef" to questionRef.take(500),
        "errorCategory" to errorCategory,
        "mistakeExplanation" to mistakeExplanation.take(2000),
        "correctApproach" to correctApproach.take(2000),
        "importantLesson" to importantLesson.take(1000),
        "dateRecorded" to dateRecorded.take(32),
        "reattemptStatus" to reattemptStatus,
        "scheduledRevisionDate" to scheduledRevisionDate.take(32),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class FormulaNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subject: String = Subject.PHYSICS.name,
    val chapterId: String = "",
    val chapterName: String = "",
    val noteType: String = NoteType.FORMULA.name,
    val title: String = "",
    val content: String = "",
    val latexFormula: String = "",
    val attachmentUri: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val subjectEnum: Subject get() = Subject.fromString(subject)
    val noteTypeEnum: NoteType get() = NoteType.fromString(noteType)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "subject" to subject,
        "chapterId" to chapterId.take(128),
        "chapterName" to chapterName.take(120),
        "noteType" to noteType,
        "title" to title.take(150),
        "content" to content.take(4000),
        "latexFormula" to latexFormula.take(2000),
        "attachmentUri" to attachmentUri.take(1000),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
