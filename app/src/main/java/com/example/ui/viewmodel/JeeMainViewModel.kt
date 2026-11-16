package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.PracticeQuestionGenerator
import com.example.data.firestore.JeeRepository
import com.example.domain.engine.BackupBundle
import com.example.domain.engine.BackupSerializer
import com.example.domain.engine.IstDateUtils
import com.example.domain.engine.SpacedRepetitionEngine
import com.example.domain.model.CalendarEvent
import com.example.domain.model.CalendarEventType
import com.example.domain.model.Chapter
import com.example.domain.model.ChapterStatus
import com.example.domain.model.ChemistrySection
import com.example.domain.model.ClassLevel
import com.example.domain.model.DashboardMetrics
import com.example.domain.model.ErrorCategory
import com.example.domain.model.ErrorRecord
import com.example.domain.model.EvaluatedQuestionResult
import com.example.domain.model.FormulaNote
import com.example.domain.model.MonthlyReport
import com.example.domain.model.NoteType
import com.example.domain.model.PracticeDifficultyFilter
import com.example.domain.model.PracticeQuestionType
import com.example.domain.model.PracticeTestOutcomeSummary
import com.example.domain.model.PracticeTestUiState
import com.example.domain.model.ReattemptStatus
import com.example.domain.model.RecommendationItem
import com.example.domain.model.RevisionOutcome
import com.example.domain.model.RevisionQueueItem
import com.example.domain.model.RevisionSession
import com.example.domain.model.ScheduleComputationResult
import com.example.domain.model.Subject
import com.example.domain.model.TestChapterResult
import com.example.domain.model.TestRecord
import com.example.domain.model.TestType
import com.example.domain.model.Topic
import com.example.domain.model.UserProfile
import com.example.domain.model.WeeklyReport
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class JeeAppUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val statusBanner: String? = null,
    val userProfile: UserProfile? = null,
    val chapters: List<Chapter> = emptyList(),
    val topics: List<Topic> = emptyList(),
    val sessions: List<RevisionSession> = emptyList(),
    val calendarEvents: List<CalendarEvent> = emptyList(),
    val testRecords: List<TestRecord> = emptyList(),
    val errorRecords: List<ErrorRecord> = emptyList(),
    val formulaNotes: List<FormulaNote> = emptyList(),
    val revisionQueue: List<RevisionQueueItem> = emptyList(),
    val dashboardMetrics: DashboardMetrics? = null,
    val recommendations: List<RecommendationItem> = emptyList(),
    val weeklyReport: WeeklyReport? = null,
    val monthlyReport: MonthlyReport? = null
)

class JeeMainViewModel(
    private val repository: JeeRepository,
    private val currentUserId: String,
    private val displayName: String,
    private val email: String?
) : ViewModel() {

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)

    private val questionGenerator = PracticeQuestionGenerator()
    private var practiceGenerationJob: Job? = null
    private val _practiceTestState = MutableStateFlow<PracticeTestUiState>(PracticeTestUiState.Idle)
    val practiceTestState: StateFlow<PracticeTestUiState> = _practiceTestState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.ensureUserInitialized(displayName, email)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Initialization failed", e)
                _errorState.value = unwrapError(e)
            }
        }
    }

    private fun <T> safeStream(
        flow: kotlinx.coroutines.flow.Flow<T>,
        fallback: T
    ): kotlinx.coroutines.flow.Flow<T> =
        flow.catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Stream error", root)
            _errorState.value = root.localizedMessage ?: "Sync error"
            emit(fallback)
        }

    val uiState: StateFlow<JeeAppUiState> = combine(
        safeStream(repository.observeUserProfile(currentUserId), null),
        safeStream(repository.observeChapters(currentUserId), emptyList()),
        safeStream(repository.observeTopics(currentUserId), emptyList()),
        safeStream(repository.observeRevisionSessions(currentUserId), emptyList()),
        safeStream(repository.observeCalendarEvents(currentUserId), emptyList()),
        safeStream(repository.observeTestRecords(currentUserId), emptyList()),
        safeStream(repository.observeErrorRecords(currentUserId), emptyList()),
        safeStream(repository.observeFormulaNotes(currentUserId), emptyList()),
        _errorState,
        _statusBanner
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val profile = values[0] as UserProfile?
        @Suppress("UNCHECKED_CAST")
        val chapters = values[1] as List<Chapter>
        @Suppress("UNCHECKED_CAST")
        val topics = values[2] as List<Topic>
        @Suppress("UNCHECKED_CAST")
        val sessions = values[3] as List<RevisionSession>
        @Suppress("UNCHECKED_CAST")
        val events = values[4] as List<CalendarEvent>
        @Suppress("UNCHECKED_CAST")
        val tests = values[5] as List<TestRecord>
        @Suppress("UNCHECKED_CAST")
        val errors = values[6] as List<ErrorRecord>
        @Suppress("UNCHECKED_CAST")
        val notes = values[7] as List<FormulaNote>
        val errMsg = values[8] as String?
        val banner = values[9] as String?

        val today = IstDateUtils.todayIst()
        val queue = SpacedRepetitionEngine.buildRevisionQueue(
            chapters = chapters,
            topics = topics,
            calendarEvents = events,
            todayDate = today,
            includeUpcoming = true
        )
        val metrics = SpacedRepetitionEngine.computeDashboardMetrics(
            chapters = chapters,
            topics = topics,
            sessions = sessions,
            calendarEvents = events,
            todayDate = today
        )
        val recs = SpacedRepetitionEngine.generateRecommendations(
            chapters = chapters,
            topics = topics,
            sessions = sessions,
            errors = errors,
            testRecords = tests
        )
        val weekly = SpacedRepetitionEngine.generateWeeklyReport(chapters, sessions, errors, today)
        val monthly = SpacedRepetitionEngine.generateMonthlyReport(chapters, sessions, errors, today)

        JeeAppUiState(
            isLoading = profile == null && chapters.isEmpty() && errMsg == null,
            errorMessage = errMsg,
            statusBanner = banner,
            userProfile = profile,
            chapters = chapters,
            topics = topics,
            sessions = sessions,
            calendarEvents = events,
            testRecords = tests,
            errorRecords = errors,
            formulaNotes = notes,
            revisionQueue = queue,
            dashboardMetrics = metrics,
            recommendations = recs,
            weeklyReport = weekly,
            monthlyReport = monthly
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = JeeAppUiState(isLoading = true)
    )

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun clearError() {
        _errorState.value = null
    }

    private fun launchMutation(successMessage: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                if (successMessage != null) _statusBanner.value = successMessage
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Mutation failed", e)
                _errorState.value = unwrapError(e)
            }
        }
    }

    private fun unwrapError(e: Throwable): String {
        val fs = generateSequence(e) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        return fs?.localizedMessage ?: e.localizedMessage ?: "Operation failed"
    }

    // ================= REVISION ENGINE ACTIONS =================

    fun previewNextSchedule(
        chapter: Chapter,
        topic: Topic?,
        outcome: RevisionOutcome,
        understanding: Int,
        confidence: Int,
        difficulty: Int,
        questionsAttempted: Int,
        questionsCorrect: Int,
        questionsWithHints: Int,
        questionsUnsolved: Int,
        durationMinutes: Int
    ): ScheduleComputationResult {
        val state = uiState.value
        val profile = state.userProfile
        val intervals = profile?.intervalsAsInts() ?: listOf(1, 3, 7, 14, 30, 60, 90)
        val capacity = profile?.dailyRevisionCapacity?.toInt() ?: 6
        val blocked = profile?.blockedDates?.toSet() ?: emptySet()
        val today = IstDateUtils.todayIst()

        val scheduledCounts = buildDateCountMap(state.chapters, state.topics)
        val prevOutcomes = state.sessions
            .filter {
                it.chapterId == chapter.id && (topic == null || it.topicId == topic.id)
            }
            .sortedByDescending { it.sessionDate }
            .map { it.outcomeEnum }

        return SpacedRepetitionEngine.computeNextSchedule(
            currentIntervalDays = (topic?.currentIntervalDays ?: chapter.currentIntervalDays).toInt(),
            consecutiveSuccesses = (topic?.consecutiveSuccesses ?: chapter.consecutiveSuccesses).toInt(),
            outcome = outcome,
            understanding = understanding,
            confidence = confidence,
            difficulty = difficulty,
            questionsAttempted = questionsAttempted,
            questionsCorrect = questionsCorrect,
            questionsWithHints = questionsWithHints,
            questionsUnsolved = questionsUnsolved,
            timeSpentMinutes = durationMinutes,
            previousOutcomes = prevOutcomes,
            customIntervals = intervals,
            todayDate = today,
            dailyCapacity = capacity,
            scheduledCountsByDate = scheduledCounts,
            blockedDates = blocked,
            cumulativeChapterQuestions = chapter.totalQuestions.toInt()
        )
    }

    private fun buildDateCountMap(chapters: List<Chapter>, topics: List<Topic>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        chapters.filter { !it.isArchived && it.nextRevisionDate.isNotBlank() }.forEach {
            map[it.nextRevisionDate] = (map[it.nextRevisionDate] ?: 0) + 1
        }
        topics.filter { !it.isArchived && it.nextRevisionDate.isNotBlank() }.forEach {
            map[it.nextRevisionDate] = (map[it.nextRevisionDate] ?: 0) + 1
        }
        return map
    }

    fun submitRevisionSession(
        chapter: Chapter,
        topic: Topic?,
        durationMinutes: Int,
        activeRecallNotes: String,
        theoryChecklist: List<String>,
        questionsAttempted: Int,
        questionsCorrect: Int,
        questionsIncorrect: Int,
        questionsWithHints: Int,
        questionsUnsolved: Int,
        whatForgot: String,
        unclearConcept: String,
        mistakeMade: String,
        needsPractice: String,
        understanding: Int,
        confidence: Int,
        difficulty: Int,
        outcome: RevisionOutcome,
        saveToErrorNotebook: Boolean,
        errorCategory: ErrorCategory,
        onResult: (ScheduleComputationResult) -> Unit
    ) {
        val schedule = previewNextSchedule(
            chapter = chapter,
            topic = topic,
            outcome = outcome,
            understanding = understanding,
            confidence = confidence,
            difficulty = difficulty,
            questionsAttempted = questionsAttempted,
            questionsCorrect = questionsCorrect,
            questionsWithHints = questionsWithHints,
            questionsUnsolved = questionsUnsolved,
            durationMinutes = durationMinutes
        )
        val today = IstDateUtils.todayIst()
        val prevInterval = (topic?.currentIntervalDays ?: chapter.currentIntervalDays)

        val session = RevisionSession(
            userId = currentUserId,
            chapterId = chapter.id,
            chapterName = chapter.name,
            topicId = topic?.id ?: "",
            topicName = topic?.name ?: "",
            subject = chapter.subject,
            sessionDate = today,
            durationMinutes = durationMinutes.toLong(),
            activeRecallNotes = activeRecallNotes,
            theoryChecklist = theoryChecklist.joinToString(","),
            questionsAttempted = questionsAttempted.toLong(),
            questionsCorrect = questionsCorrect.toLong(),
            questionsIncorrect = questionsIncorrect.toLong(),
            questionsWithHints = questionsWithHints.toLong(),
            questionsUnsolved = questionsUnsolved.toLong(),
            whatForgot = whatForgot,
            unclearConcept = unclearConcept,
            mistakeMade = mistakeMade,
            needsPractice = needsPractice,
            understandingRating = understanding.toLong(),
            confidenceRating = confidence.toLong(),
            difficultyRating = difficulty.toLong(),
            outcome = outcome.name,
            previousIntervalDays = prevInterval,
            newIntervalDays = schedule.nextIntervalDays.toLong(),
            nextRevisionDate = schedule.nextRevisionDate,
            rescheduleExplanation = schedule.explanation
        )

        val optionalError = if (saveToErrorNotebook && (mistakeMade.isNotBlank() || whatForgot.isNotBlank() || unclearConcept.isNotBlank())) {
            ErrorRecord(
                userId = currentUserId,
                subject = chapter.subject,
                chapterId = chapter.id,
                chapterName = chapter.name,
                topicId = topic?.id ?: "",
                topicName = topic?.name ?: "",
                questionRef = "Revision Session (${IstDateUtils.formatShort(today)})",
                errorCategory = errorCategory.name,
                mistakeExplanation = mistakeMade.ifBlank { whatForgot }.ifBlank { unclearConcept },
                correctApproach = unclearConcept.ifBlank { "Review core theory & re-solve without hints" },
                importantLesson = needsPractice.ifBlank { whatForgot },
                dateRecorded = today,
                reattemptStatus = ReattemptStatus.PENDING.name
            )
        } else null

        launchMutation("Revision logged! Next review: ${IstDateUtils.formatShort(schedule.nextRevisionDate)}") {
            repository.recordRevisionSession(session, chapter, topic, schedule, optionalError)
            onResult(schedule)
        }
    }

    fun rescheduleItem(item: RevisionQueueItem, newDate: String, reason: String) {
        val cleanReason = reason.ifBlank { "Manually rescheduled by student" }
        val original = item.originalDueDate.ifBlank { item.dueDate }
        launchMutation("Rescheduled to ${IstDateUtils.formatShort(newDate)}") {
            if (item.topicId != null) {
                repository.updateTopicFields(
                    item.topicId,
                    mapOf(
                        "nextRevisionDate" to newDate,
                        "originalDueDate" to original,
                        "lastRescheduleReason" to cleanReason,
                        "skipReason" to "",
                        "skippedUntilDate" to ""
                    )
                )
            } else {
                repository.updateChapterFields(
                    item.chapterId,
                    mapOf(
                        "nextRevisionDate" to newDate,
                        "originalDueDate" to original,
                        "lastRescheduleReason" to cleanReason,
                        "skipReason" to "",
                        "skippedUntilDate" to ""
                    )
                )
            }
        }
    }

    fun skipQueueItem(item: RevisionQueueItem, reason: String) {
        val tomorrow = IstDateUtils.addDays(IstDateUtils.todayIst(), 1)
        val original = item.originalDueDate.ifBlank { item.dueDate }
        launchMutation("Deferred to tomorrow with reason logged") {
            if (item.topicId != null) {
                repository.updateTopicFields(
                    item.topicId,
                    mapOf(
                        "skipReason" to reason.take(300),
                        "skippedUntilDate" to tomorrow,
                        "nextRevisionDate" to tomorrow,
                        "originalDueDate" to original,
                        "lastRescheduleReason" to "Skipped: ${reason.take(200)}"
                    )
                )
            } else {
                repository.updateChapterFields(
                    item.chapterId,
                    mapOf(
                        "skipReason" to reason.take(300),
                        "skippedUntilDate" to tomorrow,
                        "nextRevisionDate" to tomorrow,
                        "originalDueDate" to original,
                        "lastRescheduleReason" to "Skipped: ${reason.take(200)}"
                    )
                )
            }
        }
    }

    // ================= CHAPTERS & TOPICS CRUD =================

    fun addChapter(
        name: String,
        subject: Subject,
        section: ChemistrySection,
        classLevel: ClassLevel,
        importance: Int
    ) {
        val existingCount = uiState.value.chapters.count { it.subjectEnum == subject }
        val syllabusVersion = uiState.value.userProfile?.syllabusVersion ?: "JEE-2026-v1"
        launchMutation("Added chapter '$name'") {
            repository.createChapter(
                Chapter(
                    userId = currentUserId,
                    name = name.trim(),
                    subject = subject.name,
                    section = if (subject == Subject.CHEMISTRY) section.firestoreValue else "",
                    classLevel = classLevel.name,
                    orderIndex = (existingCount + 1).toLong(),
                    syllabusVersion = syllabusVersion,
                    importance = importance.toLong()
                )
            )
        }
    }

    fun renameChapter(chapter: Chapter, newName: String) {
        launchMutation("Renamed chapter") {
            repository.updateChapterFields(chapter.id, mapOf("name" to newName.trim().take(120)))
        }
    }

    fun toggleArchiveChapter(chapter: Chapter) {
        val next = !chapter.isArchived
        launchMutation(if (next) "Chapter archived" else "Chapter restored") {
            repository.updateChapterFields(chapter.id, mapOf("isArchived" to next))
        }
    }

    fun swapChapterOrder(first: Chapter, second: Chapter) {
        launchMutation {
            repository.updateChapterFields(first.id, mapOf("orderIndex" to second.orderIndex))
            repository.updateChapterFields(second.id, mapOf("orderIndex" to first.orderIndex))
        }
    }

    fun updateChapterStatus(chapter: Chapter, newStatus: ChapterStatus) {
        val today = IstDateUtils.todayIst()
        val firstInterval = uiState.value.userProfile?.intervalsAsInts()?.firstOrNull() ?: 1
        val updates = mutableMapOf<String, Any>("status" to newStatus.name)
        if (newStatus != ChapterStatus.NOT_STARTED && chapter.initialLearningDate.isBlank()) {
            updates["initialLearningDate"] = today
        }
        if ((newStatus == ChapterStatus.THEORY_COMPLETED || newStatus == ChapterStatus.REVISION_IN_PROGRESS || newStatus == ChapterStatus.CURRENTLY_STUDYING)
            && chapter.nextRevisionDate.isBlank()
        ) {
            val nextDate = IstDateUtils.addDays(today, firstInterval)
            updates["nextRevisionDate"] = nextDate
            updates["originalDueDate"] = nextDate
            updates["currentIntervalDays"] = firstInterval.toLong()
            updates["lastRescheduleReason"] = "Auto-scheduled Day $firstInterval after marking ${newStatus.label}"
        }
        launchMutation("Status updated to ${newStatus.label}") {
            repository.updateChapterFields(chapter.id, updates)
        }
    }

    fun updateChapterNotesAndRatings(
        chapter: Chapter,
        personalNotes: String,
        importantFormulas: String,
        commonMistakes: String,
        difficultQuestions: String,
        importance: Int
    ) {
        launchMutation("Chapter profile saved") {
            repository.updateChapterFields(
                chapter.id,
                mapOf(
                    "personalNotes" to personalNotes.take(4000),
                    "importantFormulas" to importantFormulas.take(4000),
                    "commonMistakes" to commonMistakes.take(4000),
                    "difficultQuestions" to difficultQuestions.take(4000),
                    "importance" to importance.toLong().coerceIn(1L, 5L)
                )
            )
        }
    }

    fun scheduleExtraChapterRevision(chapterId: String, reason: String) {
        val today = IstDateUtils.todayIst()
        launchMutation("Scheduled urgent revision for today") {
            repository.updateChapterFields(
                chapterId,
                mapOf(
                    "status" to ChapterStatus.NEEDS_ATTENTION.name,
                    "nextRevisionDate" to today,
                    "originalDueDate" to today,
                    "lastRescheduleReason" to reason.take(500)
                )
            )
        }
    }

    fun addTopic(chapter: Chapter, topicName: String) {
        val count = uiState.value.topics.count { it.chapterId == chapter.id }
        launchMutation("Added topic '$topicName'") {
            repository.createTopic(
                Topic(
                    userId = currentUserId,
                    chapterId = chapter.id,
                    subject = chapter.subject,
                    name = topicName.trim(),
                    orderIndex = (count + 1).toLong()
                )
            )
        }
    }

    fun renameTopic(topic: Topic, newName: String) {
        launchMutation("Topic renamed") {
            repository.updateTopicFields(topic.id, mapOf("name" to newName.trim().take(150)))
        }
    }

    fun toggleArchiveTopic(topic: Topic) {
        launchMutation(if (!topic.isArchived) "Topic archived" else "Topic restored") {
            repository.updateTopicFields(topic.id, mapOf("isArchived" to !topic.isArchived))
        }
    }

    fun swapTopicOrder(first: Topic, second: Topic) {
        launchMutation {
            repository.updateTopicFields(first.id, mapOf("orderIndex" to second.orderIndex))
            repository.updateTopicFields(second.id, mapOf("orderIndex" to first.orderIndex))
        }
    }

    fun updateTopicStatus(topic: Topic, status: ChapterStatus) {
        val today = IstDateUtils.todayIst()
        val firstInterval = uiState.value.userProfile?.intervalsAsInts()?.firstOrNull() ?: 1
        val updates = mutableMapOf<String, Any>("status" to status.name)
        if (status != ChapterStatus.NOT_STARTED && topic.initialLearningDate.isBlank()) {
            updates["initialLearningDate"] = today
        }
        if (status != ChapterStatus.NOT_STARTED && topic.nextRevisionDate.isBlank()) {
            val nextDate = IstDateUtils.addDays(today, firstInterval)
            updates["nextRevisionDate"] = nextDate
            updates["originalDueDate"] = nextDate
            updates["currentIntervalDays"] = firstInterval.toLong()
            updates["lastRescheduleReason"] = "Scheduled Day $firstInterval on topic start"
        }
        launchMutation("Topic status updated") {
            repository.updateTopicFields(topic.id, updates)
        }
    }

    // ================= CALENDAR & PLANNER =================

    fun toggleBlockedDate(isoDate: String) {
        val current = uiState.value.userProfile?.blockedDates ?: emptyList()
        val updated = if (current.contains(isoDate)) current - isoDate else (current + isoDate).takeLast(120)
        launchMutation(
            if (current.contains(isoDate)) "Unblocked $isoDate" else "Marked $isoDate unavailable"
        ) {
            repository.updateUserPreferences(mapOf("blockedDates" to updated))
        }
    }

    fun addCalendarEvent(
        title: String,
        eventType: CalendarEventType,
        subject: String,
        date: String,
        timeSlot: String,
        chapterId: String,
        notes: String
    ) {
        launchMutation("Added calendar event") {
            repository.createCalendarEvent(
                CalendarEvent(
                    userId = currentUserId,
                    title = title.trim(),
                    eventType = eventType.name,
                    subject = subject,
                    date = date,
                    timeSlot = timeSlot.ifBlank { "18:00" },
                    chapterId = chapterId,
                    notes = notes.trim()
                )
            )
        }
    }

    fun toggleCalendarEventDone(event: CalendarEvent) {
        launchMutation {
            repository.updateCalendarEvent(event.id, mapOf("isCompleted" to !event.isCompleted))
        }
    }

    fun deleteCalendarEvent(eventId: String) {
        launchMutation("Removed event") {
            repository.deleteCalendarEvent(eventId)
        }
    }

    fun autoBalanceCalendar() {
        val state = uiState.value
        val capacity = state.userProfile?.dailyRevisionCapacity?.toInt() ?: 6
        val blocked = state.userProfile?.blockedDates?.toSet() ?: emptySet()
        val moves = SpacedRepetitionEngine.computeLoadBalancedMoves(
            chapters = state.chapters,
            dailyCapacity = capacity,
            blockedDates = blocked
        )
        if (moves.isEmpty()) {
            _statusBanner.value = "All upcoming days are within your daily capacity of $capacity."
            return
        }
        launchMutation("Load-balanced ${moves.size} revision(s) across available days") {
            moves.forEach { (ch, newDate) ->
                val orig = ch.originalDueDate.ifBlank { ch.nextRevisionDate }
                repository.updateChapterFields(
                    ch.id,
                    mapOf(
                        "nextRevisionDate" to newDate,
                        "originalDueDate" to orig,
                        "lastRescheduleReason" to "Load-balanced from ${IstDateUtils.formatShort(orig)} to respect capacity/blocked days"
                    )
                )
            }
        }
    }

    // ================= TEST ANALYSIS =================

    fun addTestRecord(
        testName: String,
        testDate: String,
        testType: TestType,
        physicsScore: Double,
        physicsMax: Double,
        chemistryScore: Double,
        chemistryMax: Double,
        mathsScore: Double,
        mathsMax: Double,
        accuracyPercent: Double,
        timeManagementNotes: String,
        chapterResults: List<TestChapterResult>
    ) {
        val total = physicsScore + chemistryScore + mathsScore
        val maxTotal = physicsMax + chemistryMax + mathsMax
        launchMutation("Test analysis saved") {
            repository.createTestRecord(
                TestRecord(
                    userId = currentUserId,
                    testName = testName.trim(),
                    testDate = testDate,
                    testType = testType.name,
                    physicsScore = physicsScore,
                    physicsMax = physicsMax,
                    chemistryScore = chemistryScore,
                    chemistryMax = chemistryMax,
                    mathsScore = mathsScore,
                    mathsMax = mathsMax,
                    totalMarks = total,
                    maxMarks = maxTotal,
                    accuracyPercent = accuracyPercent.coerceIn(0.0, 100.0),
                    timeManagementNotes = timeManagementNotes.trim(),
                    chapterResults = chapterResults.map { it.toMap() }
                )
            )
            // Automatically flag weak chapters from test analysis as Needs Attention if accuracy < 55%
            chapterResults.filter { it.weakFlag || it.accuracyPercent < 55.0 }.forEach { weak ->
                if (weak.chapterId.isNotBlank()) {
                    repository.updateChapterFields(
                        weak.chapterId,
                        mapOf(
                            "status" to ChapterStatus.NEEDS_ATTENTION.name,
                            "nextRevisionDate" to IstDateUtils.todayIst(),
                            "originalDueDate" to IstDateUtils.todayIst(),
                            "lastRescheduleReason" to "Flagged from $testName (${weak.accuracyPercent.toInt()}% accuracy)"
                        )
                    )
                }
            }
        }
    }

    fun deleteTestRecord(testId: String) {
        launchMutation("Test record deleted") {
            repository.deleteTestRecord(testId)
        }
    }

    // ================= ERROR NOTEBOOK =================

    fun addErrorRecord(
        subject: Subject,
        chapter: Chapter?,
        topicName: String,
        questionRef: String,
        category: ErrorCategory,
        mistakeExplanation: String,
        correctApproach: String,
        importantLesson: String
    ) {
        val today = IstDateUtils.todayIst()
        launchMutation("Saved to Error Notebook") {
            repository.createErrorRecord(
                ErrorRecord(
                    userId = currentUserId,
                    subject = subject.name,
                    chapterId = chapter?.id ?: "",
                    chapterName = chapter?.name ?: "General",
                    topicId = "",
                    topicName = topicName.trim(),
                    questionRef = questionRef.trim(),
                    errorCategory = category.name,
                    mistakeExplanation = mistakeExplanation.trim(),
                    correctApproach = correctApproach.trim(),
                    importantLesson = importantLesson.trim(),
                    dateRecorded = today,
                    reattemptStatus = ReattemptStatus.PENDING.name
                )
            )
        }
    }

    fun updateErrorStatus(error: ErrorRecord, status: ReattemptStatus) {
        launchMutation("Error status: ${status.label}") {
            repository.updateErrorRecord(error.id, mapOf("reattemptStatus" to status.name))
        }
    }

    fun scheduleErrorForRevision(error: ErrorRecord) {
        val today = IstDateUtils.todayIst()
        launchMutation("Scheduled error & chapter for revision today") {
            repository.updateErrorRecord(
                error.id,
                mapOf(
                    "reattemptStatus" to ReattemptStatus.SCHEDULED.name,
                    "scheduledRevisionDate" to today
                )
            )
            if (error.chapterId.isNotBlank()) {
                repository.updateChapterFields(
                    error.chapterId,
                    mapOf(
                        "nextRevisionDate" to today,
                        "originalDueDate" to today,
                        "status" to ChapterStatus.NEEDS_ATTENTION.name,
                        "lastRescheduleReason" to "Scheduled from Error Notebook: ${error.questionRef}"
                    )
                )
            }
        }
    }

    fun deleteErrorRecord(errorId: String) {
        launchMutation("Error entry removed") {
            repository.deleteErrorRecord(errorId)
        }
    }

    // ================= FORMULA BANK =================

    fun addFormulaNote(
        subject: Subject,
        chapter: Chapter?,
        noteType: NoteType,
        title: String,
        content: String,
        latexFormula: String,
        attachmentUri: String
    ) {
        launchMutation("Saved to ${subject.label} Formula Bank") {
            repository.createFormulaNote(
                FormulaNote(
                    userId = currentUserId,
                    subject = subject.name,
                    chapterId = chapter?.id ?: "",
                    chapterName = chapter?.name ?: "General",
                    noteType = noteType.name,
                    title = title.trim(),
                    content = content.trim(),
                    latexFormula = latexFormula.trim(),
                    attachmentUri = attachmentUri
                )
            )
        }
    }

    fun deleteFormulaNote(noteId: String) {
        launchMutation("Note deleted") {
            repository.deleteFormulaNote(noteId)
        }
    }

    // ================= SETTINGS, EXPORT, RESTORE =================

    fun updateSettings(updates: Map<String, Any>, message: String = "Settings saved") {
        launchMutation(message) {
            repository.updateUserPreferences(updates)
        }
    }

    fun buildJsonBackup(): String {
        val s = uiState.value
        return BackupSerializer.toJsonString(
            profile = s.userProfile,
            chapters = s.chapters,
            topics = s.topics,
            sessions = s.sessions,
            events = s.calendarEvents,
            tests = s.testRecords,
            errors = s.errorRecords,
            notes = s.formulaNotes
        )
    }

    fun buildCsvExport(): String {
        val s = uiState.value
        return BackupSerializer.toCsvSummary(s.chapters, s.sessions)
    }

    fun restoreBackup(bundle: BackupBundle) {
        launchMutation("Backup restored (${bundle.chapters.size} chapters, ${bundle.sessions.size} sessions)") {
            repository.restoreFromBackup(bundle)
        }
    }

    // ================= AI CHAPTER & TOPIC PRACTICE TESTS =================

    fun generatePracticeTest(
        chapter: Chapter,
        topic: Topic?,
        questionCount: Int,
        difficultyFilter: PracticeDifficultyFilter,
        allowedTypes: Set<PracticeQuestionType>
    ) {
        practiceGenerationJob?.cancel()
        val safeCount = questionCount.coerceIn(1, 20)
        _practiceTestState.value = PracticeTestUiState.Generating(
            chapterName = chapter.name,
            topicName = topic?.name,
            requestedCount = safeCount
        )
        practiceGenerationJob = viewModelScope.launch {
            try {
                val questions = questionGenerator.generateQuestions(
                    chapter = chapter,
                    topic = topic,
                    questionCount = safeCount,
                    difficultyFilter = difficultyFilter,
                    allowedTypes = allowedTypes
                )
                _practiceTestState.value = PracticeTestUiState.InProgress(
                    chapter = chapter,
                    topic = topic,
                    questions = questions,
                    currentIndex = 0,
                    userAnswers = emptyMap(),
                    elapsedSeconds = 0
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Practice test generation failed", e)
                _practiceTestState.value = PracticeTestUiState.Error(
                    e.localizedMessage ?: "Failed to generate AI practice questions. Please try again."
                )
            }
        }
    }

    fun updatePracticeAnswer(questionId: String, answer: String) {
        val current = _practiceTestState.value as? PracticeTestUiState.InProgress ?: return
        _practiceTestState.value = current.copy(
            userAnswers = current.userAnswers + (questionId to answer)
        )
    }

    fun goToPracticeQuestion(index: Int) {
        val current = _practiceTestState.value as? PracticeTestUiState.InProgress ?: return
        if (current.questions.isEmpty()) return
        _practiceTestState.value = current.copy(
            currentIndex = index.coerceIn(0, current.questions.lastIndex)
        )
    }

    fun submitPracticeTest(elapsedSeconds: Int) {
        val current = _practiceTestState.value as? PracticeTestUiState.InProgress ?: return
        val evaluated = current.questions.map { q ->
            SpacedRepetitionEngine.evaluatePracticeAnswer(q, current.userAnswers[q.id].orEmpty())
        }
        val state = uiState.value
        val profile = state.userProfile
        val intervals = profile?.intervalsAsInts() ?: listOf(1, 3, 7, 14, 30, 60, 90)
        val capacity = profile?.dailyRevisionCapacity?.toInt() ?: 6
        val blocked = profile?.blockedDates?.toSet() ?: emptySet()
        val today = IstDateUtils.todayIst()
        val scheduledCounts = buildDateCountMap(state.chapters, state.topics)

        val impact = SpacedRepetitionEngine.computeChapterTestRevisionImpact(
            chapter = current.chapter,
            topic = current.topic,
            results = evaluated,
            customIntervals = intervals,
            todayDate = today,
            dailyCapacity = capacity,
            scheduledCountsByDate = scheduledCounts,
            blockedDates = blocked
        )

        val durationMinutes = (elapsedSeconds / 60).coerceAtLeast(1)
        val correctCount = evaluated.count { it.isCorrect }
        val totalCount = evaluated.size
        val targetName = current.topic?.name ?: current.chapter.name
        val testTitle = "Practice Test: $targetName"

        val summary = PracticeTestOutcomeSummary(
            chapterId = current.chapter.id,
            chapterName = current.chapter.name,
            topicId = current.topic?.id,
            topicName = current.topic?.name,
            totalQuestions = totalCount,
            correctCount = correctCount,
            accuracyPercent = impact.accuracyPercent,
            durationMinutes = durationMinutes,
            triggeredEarlierRevision = impact.triggeredEarlierRevision,
            nextRevisionDate = impact.scheduleResult.nextRevisionDate,
            rescheduleExplanation = impact.scheduleResult.explanation,
            weakConceptTags = impact.weakConceptTags,
            questionResults = evaluated
        )

        viewModelScope.launch {
            try {
                repository.recordChapterPracticeTest(
                    chapter = current.chapter,
                    topic = current.topic,
                    impact = impact,
                    durationMinutes = durationMinutes,
                    testTitle = testTitle
                )
                _practiceTestState.value = PracticeTestUiState.Completed(summary)
                _statusBanner.value = if (impact.triggeredEarlierRevision) {
                    "Score ${impact.accuracyPercent.roundToInt()}% — earlier revision scheduled for ${IstDateUtils.formatShort(impact.scheduleResult.nextRevisionDate)}"
                } else {
                    "Practice test saved (${impact.accuracyPercent.roundToInt()}% accuracy)"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save practice test results", e)
                _errorState.value = unwrapError(e)
                _practiceTestState.value = PracticeTestUiState.Completed(summary)
            }
        }
    }

    fun saveMissedQuestionToErrorNotebook(
        chapter: Chapter,
        topic: Topic?,
        evaluated: EvaluatedQuestionResult
    ) {
        val today = IstDateUtils.todayIst()
        val refText = "${evaluated.question.conceptTag}: ${evaluated.question.prompt}"
            .take(480)
            .ifBlank { "AI Practice Question" }
        val userNote = evaluated.userAnswer.ifBlank { "Unanswered" }
        launchMutation("Saved question to Error Notebook") {
            repository.createErrorRecord(
                ErrorRecord(
                    userId = currentUserId,
                    subject = chapter.subject,
                    chapterId = chapter.id,
                    chapterName = chapter.name,
                    topicId = topic?.id ?: "",
                    topicName = topic?.name ?: "",
                    questionRef = refText,
                    errorCategory = ErrorCategory.CONCEPTUAL.name,
                    mistakeExplanation = "AI Practice Test (${evaluated.question.difficulty.label} ${evaluated.question.type.label}). Your answer: $userNote".take(1950),
                    correctApproach = evaluated.question.solutionSteps.take(1950),
                    importantLesson = "Correct answer: ${evaluated.question.correctAnswer}".take(950),
                    dateRecorded = today,
                    reattemptStatus = ReattemptStatus.PENDING.name
                )
            )
        }
    }

    fun resetPracticeTest() {
        practiceGenerationJob?.cancel()
        practiceGenerationJob = null
        _practiceTestState.value = PracticeTestUiState.Idle
    }

    private companion object {
        const val TAG = "JeeMainVM"
    }
}
