package com.example.domain.engine

import com.example.domain.model.AlertItem
import com.example.domain.model.AlertSeverity
import com.example.domain.model.CalendarEvent
import com.example.domain.model.CalendarEventType
import com.example.domain.model.Chapter
import com.example.domain.model.ChapterStatus
import com.example.domain.model.ChapterTestScheduleImpact
import com.example.domain.model.DashboardMetrics
import com.example.domain.model.ErrorCategory
import com.example.domain.model.ErrorRecord
import com.example.domain.model.EvaluatedQuestionResult
import com.example.domain.model.MonthlyReport
import com.example.domain.model.PracticeQuestion
import com.example.domain.model.PracticeQuestionType
import com.example.domain.model.QueueCategory
import com.example.domain.model.RecommendationItem
import com.example.domain.model.RevisionOutcome
import com.example.domain.model.RevisionQueueItem
import com.example.domain.model.RevisionSession
import com.example.domain.model.ScheduleComputationResult
import com.example.domain.model.Subject
import com.example.domain.model.SubjectProgressSummary
import com.example.domain.model.TestChapterResult
import com.example.domain.model.TestRecord
import com.example.domain.model.Topic
import com.example.domain.model.UserProfile
import com.example.domain.model.WeeklyReport
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Core deterministic spaced repetition, intelligent queue sorting, retention heuristic,
 * calendar load balancing, and data-driven analytics engine.
 * Never fabricates statistics when user records are absent.
 */
object SpacedRepetitionEngine {

    /**
     * Computes adaptive next revision date, interval, new status, heuristic retention,
     * and transparent explanation after a revision session.
     */
    fun computeNextSchedule(
        currentIntervalDays: Int,
        consecutiveSuccesses: Int,
        outcome: RevisionOutcome,
        understanding: Int,
        confidence: Int,
        difficulty: Int,
        questionsAttempted: Int,
        questionsCorrect: Int,
        questionsWithHints: Int,
        questionsUnsolved: Int,
        timeSpentMinutes: Int,
        previousOutcomes: List<RevisionOutcome>,
        customIntervals: List<Int>,
        todayDate: String = IstDateUtils.todayIst(),
        dailyCapacity: Int = 6,
        scheduledCountsByDate: Map<String, Int> = emptyMap(),
        blockedDates: Set<String> = emptySet(),
        cumulativeChapterQuestions: Int = 0
    ): ScheduleComputationResult {
        val sortedIntervals = customIntervals.filter { it >= 1 }.distinct().sorted()
            .ifEmpty { listOf(1, 3, 7, 14, 30, 60, 90) }

        val safeCurrentInterval = currentIntervalDays.coerceAtLeast(0)
        val safeConsecutive = consecutiveSuccesses.coerceAtLeast(0)
        val safeUnderstanding = understanding.coerceIn(1, 5)
        val safeConfidence = confidence.coerceIn(1, 5)
        val safeDifficulty = difficulty.coerceIn(1, 5)
        val safeAttempted = questionsAttempted.coerceAtLeast(0)
        val safeCorrect = questionsCorrect.coerceIn(0, safeAttempted)
        val safeHints = questionsWithHints.coerceIn(0, safeAttempted)
        val safeUnsolved = questionsUnsolved.coerceIn(0, safeAttempted)
        val safeTime = timeSpentMinutes.coerceAtLeast(0)
        val safeCapacity = dailyCapacity.coerceAtLeast(1)
        val safeCumulative = cumulativeChapterQuestions.coerceAtLeast(0)

        val accuracy = if (safeAttempted > 0) {
            (safeCorrect.toDouble() / safeAttempted.toDouble()) * 100.0
        } else {
            safeUnderstanding * 20.0
        }

        // Performance modifier from accuracy, confidence, understanding, difficulty, hints, time, history
        var modifier = 0.0
        when {
            accuracy >= 85.0 -> modifier += 0.15
            accuracy < 45.0 -> modifier -= 0.30
            accuracy < 65.0 -> modifier -= 0.15
        }
        val avgConcept = (safeUnderstanding + safeConfidence) / 2.0
        when {
            avgConcept >= 4.5 -> modifier += 0.10
            avgConcept <= 2.5 -> modifier -= 0.20
        }
        if (safeDifficulty >= 4) modifier -= 0.12
        if (safeAttempted > 0 && safeHints.toDouble() / safeAttempted > 0.35) {
            modifier -= 0.12
        }
        if (safeAttempted > 0 && safeUnsolved.toDouble() / safeAttempted > 0.30) {
            modifier -= 0.10
        }
        if (safeTime > 60 && accuracy < 65.0) modifier -= 0.08
        if (previousOutcomes.take(2).any { it == RevisionOutcome.FORGOT || it == RevisionOutcome.DIFFICULT }) {
            modifier -= 0.12
        }

        val maxConfigured = sortedIntervals.last()
        val nextStepInSeq = sortedIntervals.firstOrNull { it > safeCurrentInterval }
            ?: (maxConfigured * 1.5).roundToInt().coerceAtLeast(safeCurrentInterval + 15)
        val recentStruggle = previousOutcomes.firstOrNull().let {
            it == RevisionOutcome.DIFFICULT || it == RevisionOutcome.FORGOT
        }

        val (rawInterval, newConsecutive, baseReason) = when (outcome) {
            RevisionOutcome.EASY -> {
                val factor = (1.15 + modifier).coerceIn(1.05, 1.45)
                val target = (nextStepInSeq * factor).roundToInt().coerceIn(
                    max(safeCurrentInterval + 2, sortedIntervals.first()),
                    maxOf(maxConfigured * 2, safeCurrentInterval + 45, 180)
                )
                Triple(
                    target,
                    safeConsecutive + 1,
                    "Outcome EASY with ${accuracy.roundToInt()}% accuracy & confidence $safeConfidence/5 extended the sequence from ${safeCurrentInterval}d to ${target}d."
                )
            }

            RevisionOutcome.GOOD -> {
                val factor = (1.0 + modifier).coerceIn(0.85, 1.15)
                val target = (nextStepInSeq * factor).roundToInt().coerceIn(
                    sortedIntervals.first(),
                    maxOf((maxConfigured * 1.5).roundToInt(), safeCurrentInterval + 30)
                )
                Triple(
                    target,
                    safeConsecutive + 1,
                    "Outcome GOOD followed your spaced-repetition sequence (${safeCurrentInterval}d -> ${target}d) calibrated by ${accuracy.roundToInt()}% problem accuracy."
                )
            }

            RevisionOutcome.DIFFICULT -> {
                val fallbackShort = sortedIntervals.getOrElse(1) { min(maxConfigured, sortedIntervals.first() + 2) }
                val halved = max(sortedIntervals.first(), (safeCurrentInterval / 2.0).roundToInt())
                val target = if (accuracy < 50.0 || safeUnderstanding <= 2 || recentStruggle) {
                    sortedIntervals.first()
                } else {
                    min(fallbackShort, halved)
                }
                Triple(
                    target,
                    0,
                    "Outcome DIFFICULT (difficulty $safeDifficulty/5, hints $safeHints) pulled review earlier to ${target}d for extra practice."
                )
            }

            RevisionOutcome.FORGOT -> {
                val resetDay = sortedIntervals.first()
                Triple(
                    resetDay,
                    0,
                    "Outcome FORGOT restarted the review sequence to Day $resetDay for immediate active re-learning."
                )
            }
        }

        val originalTargetDate = IstDateUtils.addDays(todayDate, rawInterval)
        var finalDate = originalTargetDate
        var finalInterval = rawInterval
        var shifted = false
        var shiftNote = ""

        // Respect blocked dates & load-balance non-urgent reviews if day exceeds capacity
        val allowLoadBalance = outcome != RevisionOutcome.FORGOT
        val originalBlocked = blockedDates.contains(originalTargetDate)
        val originalFull = allowLoadBalance && (scheduledCountsByDate[originalTargetDate] ?: 0) >= safeCapacity
        if (originalBlocked || originalFull) {
            var chosenCandidate: String? = null
            for (offset in 1..30) {
                val candidate = IstDateUtils.addDays(originalTargetDate, offset)
                val count = scheduledCountsByDate[candidate] ?: 0
                if (!blockedDates.contains(candidate) && (!allowLoadBalance || count < safeCapacity)) {
                    chosenCandidate = candidate
                    break
                }
            }
            if (chosenCandidate == null && originalBlocked) {
                // Every day in 1..30 was full (or mostly blocked); strictly avoid blocked dates by picking least-loaded unblocked day
                chosenCandidate = (1..30)
                    .map { IstDateUtils.addDays(originalTargetDate, it) }
                    .filterNot { blockedDates.contains(it) }
                    .minByOrNull { scheduledCountsByDate[it] ?: 0 }
            }
            if (chosenCandidate != null) {
                finalDate = chosenCandidate
                finalInterval = max(1, IstDateUtils.daysBetween(todayDate, finalDate))
                shifted = true
                shiftNote = if (originalBlocked) {
                    " Shifted from $originalTargetDate (marked unavailable) to $finalDate."
                } else {
                    " Load-balanced from $originalTargetDate (daily capacity $safeCapacity full) to $finalDate."
                }
            }
        }

        // Evidence-based status (never auto-Strong without real recall + problem-solving proof)
        val totalEvidenceQuestions = safeCumulative + safeAttempted
        val heavyHintDependency = safeAttempted > 0 && (safeHints.toDouble() / safeAttempted) > 0.5
        val newStatus = when {
            outcome == RevisionOutcome.FORGOT -> ChapterStatus.NEEDS_ATTENTION
            outcome == RevisionOutcome.DIFFICULT && (accuracy < 55.0 || safeUnderstanding <= 2 || recentStruggle || heavyHintDependency) ->
                ChapterStatus.NEEDS_ATTENTION
            newConsecutive >= 2 && accuracy >= 75.0 && safeUnderstanding >= 4 && safeConfidence >= 4 && totalEvidenceQuestions >= 10 ->
                ChapterStatus.STRONG
            else -> ChapterStatus.REVISION_IN_PROGRESS
        }

        // Heuristic retention estimate immediately post-session
        val retentionScore = when (outcome) {
            RevisionOutcome.EASY -> (88.0 + (accuracy - 75.0) * 0.25).coerceIn(80.0, 98.0)
            RevisionOutcome.GOOD -> (76.0 + (accuracy - 70.0) * 0.25).coerceIn(65.0, 92.0)
            RevisionOutcome.DIFFICULT -> (52.0 + (accuracy - 50.0) * 0.25).coerceIn(38.0, 72.0)
            RevisionOutcome.FORGOT -> (28.0 + (accuracy * 0.2)).coerceIn(18.0, 48.0)
        }

        val (practiceCount, actionAdvice) = when (outcome) {
            RevisionOutcome.EASY -> {
                val count = if (safeAttempted < 5) 5 else 0
                count to "Concepts are solid; maintain speed with periodic mixed PYQs."
            }
            RevisionOutcome.GOOD -> {
                val count = if (accuracy < 75.0) 10 else 5
                count to "Follow normal sequence and solve $count mixed application problems before the next review."
            }
            RevisionOutcome.DIFFICULT -> {
                val count = when {
                    accuracy < 55.0 || safeUnderstanding <= 2 || recentStruggle -> 20
                    safeHints > 0 || safeUnsolved > 0 -> 15
                    else -> 12
                }
                count to "Assign $count targeted practice questions (focusing on hint-dependent & unsolved concepts) before reviewing in ${finalInterval}d."
            }
            RevisionOutcome.FORGOT -> {
                15 to "Run an active re-learning pass over core derivations & Error Notebook entries, then solve 15 foundational problems on Day $finalInterval."
            }
        }

        return ScheduleComputationResult(
            nextIntervalDays = finalInterval,
            nextRevisionDate = finalDate,
            originalTargetDate = originalTargetDate,
            wasLoadBalancedOrShifted = shifted,
            newConsecutiveSuccesses = newConsecutive,
            newStatus = newStatus,
            estimatedRetentionAfter = retentionScore,
            explanation = baseReason + shiftNote,
            recommendedPracticeQuestions = practiceCount,
            recommendedAction = actionAdvice
        )
    }

    /**
     * Computes current heuristic retention decay (0-100) for a studied chapter or topic.
     * Returns 0.0 when never studied/revised so zero fake progress is ever shown.
     */
    fun estimateCurrentRetention(
        lastRevisionDate: String,
        currentIntervalDays: Int,
        storedRetention: Double,
        status: ChapterStatus,
        todayDate: String = IstDateUtils.todayIst()
    ): Double {
        if (status == ChapterStatus.NOT_STARTED || lastRevisionDate.isBlank()) {
            return if (storedRetention > 0.0) storedRetention else 0.0
        }
        val elapsed = max(0, IstDateUtils.daysBetween(lastRevisionDate, todayDate))
        val stability = max(3.0, currentIntervalDays.toDouble() * 1.6)
        val decay = exp(-elapsed.toDouble() / stability)
        val base = if (storedRetention > 0.0) storedRetention else 70.0
        return (base * decay).coerceIn(5.0, 99.0)
    }

    /**
     * Collects and intelligently sorts Today's Revision Queue:
     * 1. Overdue revisions
     * 2. Due today
     * 3. Urgent weak-topic reviews (NEEDS_ATTENTION)
     * 4. Scheduled test preparation (ALLEN tests within next 5 days)
     * 5. Optional upcoming previews (next 3 days)
     */
    fun buildRevisionQueue(
        chapters: List<Chapter>,
        topics: List<Topic>,
        calendarEvents: List<CalendarEvent>,
        todayDate: String = IstDateUtils.todayIst(),
        includeUpcoming: Boolean = true
    ): List<RevisionQueueItem> {
        val items = mutableListOf<RevisionQueueItem>()
        val activeChapters = chapters.filter { !it.isArchived }
        val chapterById = activeChapters.associateBy { it.id }

        // Check upcoming tests within 5 days for Test Prep priority
        val upcomingTests = calendarEvents.filter {
            it.eventTypeEnum == CalendarEventType.ALLEN_TEST &&
                !it.isCompleted &&
                IstDateUtils.daysBetween(todayDate, it.date) in 0..5
        }

        // Topic-level scheduled items first (independent topic tracking)
        val chaptersWithScheduledTopics = mutableSetOf<String>()
        for (topic in topics.filter { !it.isArchived }) {
            val parent = chapterById[topic.chapterId] ?: continue
            if (topic.skippedUntilDate.isNotBlank() && IstDateUtils.daysBetween(todayDate, topic.skippedUntilDate) > 0) {
                continue
            }
            if (topic.nextRevisionDate.isNotBlank()) {
                val delta = IstDateUtils.daysBetween(todayDate, topic.nextRevisionDate)
                val retention = estimateCurrentRetention(
                    topic.lastRevisionDate,
                    topic.currentIntervalDays.toInt(),
                    topic.estimatedRetention,
                    topic.statusEnum,
                    todayDate
                )
                val category = when {
                    delta < 0 -> QueueCategory.OVERDUE
                    delta == 0 -> QueueCategory.DUE_TODAY
                    topic.statusEnum == ChapterStatus.NEEDS_ATTENTION && delta <= 2 -> QueueCategory.URGENT_WEAK
                    includeUpcoming && delta in 1..3 -> QueueCategory.UPCOMING
                    else -> null
                }
                if (category != null) {
                    chaptersWithScheduledTopics.add(parent.id)
                    val overdueDays = if (delta < 0) -delta else 0
                    val weaknessScore = (100.0 - retention) * 0.55
                    val categoryBoost = when (category) {
                        QueueCategory.OVERDUE -> 44.0
                        QueueCategory.URGENT_WEAK -> 36.0
                        QueueCategory.DUE_TODAY -> 32.0
                        QueueCategory.TEST_PREP -> 22.0
                        QueueCategory.UPCOMING -> 0.0
                    }
                    val priority = categoryBoost +
                        overdueDays * 20.0 +
                        (if (topic.statusEnum == ChapterStatus.NEEDS_ATTENTION) 28.0 else 0.0) +
                        weaknessScore +
                        parent.importance * 4.0
                    items.add(
                        RevisionQueueItem(
                            key = "topic_${topic.id}",
                            subject = topic.subjectEnum,
                            chapterId = parent.id,
                            chapterName = parent.name,
                            topicId = topic.id,
                            topicName = topic.name,
                            category = category,
                            dueDate = topic.nextRevisionDate,
                            originalDueDate = topic.originalDueDate.ifBlank { topic.nextRevisionDate },
                            overdueDays = overdueDays,
                            estimatedMinutes = if (topic.statusEnum == ChapterStatus.NEEDS_ATTENTION) 25 else 20,
                            estimatedRetention = retention,
                            importance = parent.importance.toInt(),
                            currentIntervalDays = topic.currentIntervalDays.toInt(),
                            status = topic.statusEnum,
                            priorityScore = priority,
                            subtitleReason = when (category) {
                                QueueCategory.OVERDUE -> "Overdue by $overdueDays day${if (overdueDays == 1) "" else "s"}"
                                QueueCategory.DUE_TODAY -> "Due today (${topic.currentIntervalDays}d interval)"
                                QueueCategory.URGENT_WEAK -> "Weak topic needs reinforcement"
                                QueueCategory.UPCOMING -> "Upcoming in $delta day${if (delta == 1) "" else "s"}"
                                QueueCategory.TEST_PREP -> "Upcoming test prep"
                            }
                        )
                    )
                }
            } else if (topic.statusEnum == ChapterStatus.NEEDS_ATTENTION) {
                chaptersWithScheduledTopics.add(parent.id)
                val retention = estimateCurrentRetention(
                    topic.lastRevisionDate,
                    topic.currentIntervalDays.toInt(),
                    topic.estimatedRetention,
                    topic.statusEnum,
                    todayDate
                )
                items.add(
                    RevisionQueueItem(
                        key = "topic_${topic.id}",
                        subject = topic.subjectEnum,
                        chapterId = parent.id,
                        chapterName = parent.name,
                        topicId = topic.id,
                        topicName = topic.name,
                        category = QueueCategory.URGENT_WEAK,
                        dueDate = todayDate,
                        originalDueDate = todayDate,
                        overdueDays = 0,
                        estimatedMinutes = 25,
                        estimatedRetention = retention,
                        importance = parent.importance.toInt(),
                        currentIntervalDays = topic.currentIntervalDays.toInt(),
                        status = topic.statusEnum,
                        priorityScore = 64.0 + (100.0 - retention) * 0.55 + parent.importance * 4.0,
                        subtitleReason = "Weak topic needs reinforcement"
                    )
                )
            }
        }

        // Chapter-level scheduled items
        for (chapter in activeChapters) {
            if (chapter.skippedUntilDate.isNotBlank() && IstDateUtils.daysBetween(todayDate, chapter.skippedUntilDate) > 0) {
                continue
            }
            val retention = estimateCurrentRetention(
                chapter.lastRevisionDate,
                chapter.currentIntervalDays.toInt(),
                chapter.estimatedRetention,
                chapter.statusEnum,
                todayDate
            )
            if (chapter.nextRevisionDate.isNotBlank()) {
                val delta = IstDateUtils.daysBetween(todayDate, chapter.nextRevisionDate)
                val isTestTarget = upcomingTests.any {
                    it.chapterId == chapter.id || it.subject == "ALL" || it.subject == chapter.subject
                } && chapter.statusEnum != ChapterStatus.NOT_STARTED && chapter.statusEnum != ChapterStatus.STRONG

                val category = when {
                    delta < 0 -> QueueCategory.OVERDUE
                    delta == 0 -> QueueCategory.DUE_TODAY
                    chapter.statusEnum == ChapterStatus.NEEDS_ATTENTION -> QueueCategory.URGENT_WEAK
                    isTestTarget && delta in 1..5 -> QueueCategory.TEST_PREP
                    includeUpcoming && delta in 1..3 -> QueueCategory.UPCOMING
                    else -> null
                }
                if (category != null) {
                    val overdueDays = if (delta < 0) -delta else 0
                    val weaknessScore = (100.0 - retention) * 0.55
                    val categoryBoost = when (category) {
                        QueueCategory.OVERDUE -> 44.0
                        QueueCategory.URGENT_WEAK -> 36.0
                        QueueCategory.DUE_TODAY -> 32.0
                        QueueCategory.TEST_PREP -> 22.0
                        QueueCategory.UPCOMING -> 0.0
                    }
                    val priority = categoryBoost +
                        overdueDays * 20.0 +
                        (if (chapter.statusEnum == ChapterStatus.NEEDS_ATTENTION) 28.0 else 0.0) +
                        weaknessScore +
                        chapter.importance * 5.0
                    items.add(
                        RevisionQueueItem(
                            key = "chapter_${chapter.id}",
                            subject = chapter.subjectEnum,
                            chapterId = chapter.id,
                            chapterName = chapter.name,
                            topicId = null,
                            topicName = null,
                            category = category,
                            dueDate = chapter.nextRevisionDate,
                            originalDueDate = chapter.originalDueDate.ifBlank { chapter.nextRevisionDate },
                            overdueDays = overdueDays,
                            estimatedMinutes = if (chapter.statusEnum == ChapterStatus.NEEDS_ATTENTION) 35 else 25,
                            estimatedRetention = retention,
                            importance = chapter.importance.toInt(),
                            currentIntervalDays = chapter.currentIntervalDays.toInt(),
                            status = chapter.statusEnum,
                            priorityScore = priority,
                            subtitleReason = when (category) {
                                QueueCategory.OVERDUE -> "Overdue by $overdueDays day${if (overdueDays == 1) "" else "s"}"
                                QueueCategory.DUE_TODAY -> "Due today (${chapter.currentIntervalDays}d cycle)"
                                QueueCategory.URGENT_WEAK -> "Needs Attention • Est. retention ${retention.roundToInt()}%"
                                QueueCategory.TEST_PREP -> "Upcoming ALLEN test preparation"
                                QueueCategory.UPCOMING -> "Preview: due ${IstDateUtils.formatShort(chapter.nextRevisionDate)}"
                            }
                        )
                    )
                }
            } else if (chapter.statusEnum == ChapterStatus.NEEDS_ATTENTION && !chaptersWithScheduledTopics.contains(chapter.id)) {
                items.add(
                    RevisionQueueItem(
                        key = "chapter_${chapter.id}",
                        subject = chapter.subjectEnum,
                        chapterId = chapter.id,
                        chapterName = chapter.name,
                        topicId = null,
                        topicName = null,
                        category = QueueCategory.URGENT_WEAK,
                        dueDate = todayDate,
                        originalDueDate = todayDate,
                        overdueDays = 0,
                        estimatedMinutes = 30,
                        estimatedRetention = retention,
                        importance = chapter.importance.toInt(),
                        currentIntervalDays = chapter.currentIntervalDays.toInt(),
                        status = chapter.statusEnum,
                        priorityScore = 64.0 + (100.0 - retention) * 0.55 + chapter.importance * 5.0,
                        subtitleReason = "Flagged as Needs Attention"
                    )
                )
            }
        }

        // Balance subjects slightly while keeping highest urgency + weakness priority items first
        return items.sortedWith(
            compareBy<RevisionQueueItem> { it.category == QueueCategory.UPCOMING }
                .thenByDescending { it.priorityScore }
                .thenBy { it.subject.ordinal }
        )
    }

    /**
     * Computes truthful Dashboard Metrics from real user data only.
     * Never invents fake statistics when no sessions or chapters have been started.
     */
    fun computeDashboardMetrics(
        chapters: List<Chapter>,
        topics: List<Topic>,
        sessions: List<RevisionSession>,
        calendarEvents: List<CalendarEvent>,
        todayDate: String = IstDateUtils.todayIst()
    ): DashboardMetrics {
        val activeChapters = chapters.filter { !it.isArchived }
        val queue = buildRevisionQueue(activeChapters, topics, calendarEvents, todayDate, includeUpcoming = false)
        val overdueItems = queue.filter { it.category == QueueCategory.OVERDUE }
        val dueTodayItems = queue.filter { it.category == QueueCategory.DUE_TODAY || it.category == QueueCategory.URGENT_WEAK }
        val completedToday = sessions.count { it.sessionDate == todayDate }

        val upcoming7Days = activeChapters.count {
            it.nextRevisionDate.isNotBlank() && IstDateUtils.daysBetween(todayDate, it.nextRevisionDate) in 1..7
        } + topics.count {
            !it.isArchived && it.nextRevisionDate.isNotBlank() && IstDateUtils.daysBetween(todayDate, it.nextRevisionDate) in 1..7
        }

        val estimatedMinutes = queue.sumOf { it.estimatedMinutes }

        // Weekly & monthly completion rates from real sessions vs due items
        val weekStart = IstDateUtils.addDays(todayDate, -6)
        val weekCompleted = sessions.count { it.sessionDate in weekStart..todayDate }
        val weekRemaining = overdueItems.size + dueTodayItems.size
        val weeklyRate = if (weekCompleted + weekRemaining > 0) {
            (weekCompleted * 100.0) / (weekCompleted + weekRemaining)
        } else null

        val monthStart = IstDateUtils.addDays(todayDate, -29)
        val monthCompleted = sessions.count { it.sessionDate in monthStart..todayDate }
        val monthlyRate = if (monthCompleted + weekRemaining > 0) {
            (monthCompleted * 100.0) / (monthCompleted + weekRemaining)
        } else null

        // Revision streak (consecutive IST days with >= 1 session)
        val sessionDates = sessions.map { it.sessionDate }.toSet()
        var streak = 0
        var checkDate = if (sessionDates.contains(todayDate)) todayDate else IstDateUtils.addDays(todayDate, -1)
        while (sessionDates.contains(checkDate)) {
            streak++
            checkDate = IstDateUtils.addDays(checkDate, -1)
        }

        val strongCount = activeChapters.count { it.statusEnum == ChapterStatus.STRONG }
        val weakCount = activeChapters.count { it.statusEnum == ChapterStatus.NEEDS_ATTENTION }

        val studiedChapters = activeChapters.filter { it.statusEnum != ChapterStatus.NOT_STARTED }
        val overallRetention = if (studiedChapters.any { it.completedRevisions > 0 || it.estimatedRetention > 0 }) {
            val retValues = studiedChapters.filter { it.completedRevisions > 0 || it.estimatedRetention > 0 }
                .map {
                    estimateCurrentRetention(
                        it.lastRevisionDate,
                        it.currentIntervalDays.toInt(),
                        it.estimatedRetention,
                        it.statusEnum,
                        todayDate
                    )
                }
            if (retValues.isNotEmpty()) retValues.average() else null
        } else null

        val subjectSummaries = Subject.entries.map { subject ->
            val subChapters = activeChapters.filter { it.subjectEnum == subject }
            val subStudied = subChapters.filter { it.statusEnum != ChapterStatus.NOT_STARTED }
            val subWithRetention = subStudied.filter { it.completedRevisions > 0 || it.estimatedRetention > 0 }
            val avgRet = if (subWithRetention.isNotEmpty()) {
                subWithRetention.map {
                    estimateCurrentRetention(
                        it.lastRevisionDate,
                        it.currentIntervalDays.toInt(),
                        it.estimatedRetention,
                        it.statusEnum,
                        todayDate
                    )
                }.average()
            } else null

            val subSessions = sessions.filter { it.subjectEnum == subject && it.questionsAttempted > 0 }
            val avgAcc = if (subSessions.isNotEmpty()) {
                val totalQ = subSessions.sumOf { it.questionsAttempted }
                val correctQ = subSessions.sumOf { it.questionsCorrect }
                if (totalQ > 0) (correctQ * 100.0) / totalQ else null
            } else null

            SubjectProgressSummary(
                subject = subject,
                totalChapters = subChapters.size,
                studiedChapters = subStudied.size,
                strongChapters = subChapters.count { it.statusEnum == ChapterStatus.STRONG },
                needsAttentionChapters = subChapters.count { it.statusEnum == ChapterStatus.NEEDS_ATTENTION },
                dueOrOverdueCount = queue.count { it.subject == subject },
                averageEstimatedRetention = avgRet,
                averageAccuracy = avgAcc
            )
        }

        // Real Alerts
        val alerts = mutableListOf<AlertItem>()
        if (overdueItems.isNotEmpty()) {
            alerts.add(
                AlertItem(
                    id = "alert_overdue",
                    title = "${overdueItems.size} Overdue Revision${if (overdueItems.size == 1) "" else "s"}",
                    subtitle = "Top priority: ${overdueItems.first().chapterName}",
                    severity = AlertSeverity.HIGH,
                    chapterId = overdueItems.first().chapterId
                )
            )
        }
        val staleChapters = activeChapters.filter {
            it.lastRevisionDate.isNotBlank() && IstDateUtils.daysBetween(it.lastRevisionDate, todayDate) >= 14
        }
        if (staleChapters.isNotEmpty()) {
            alerts.add(
                AlertItem(
                    id = "alert_stale",
                    title = "${staleChapters.size} Chapter${if (staleChapters.size == 1) "" else "s"} Not Revised in 14+ Days",
                    subtitle = staleChapters.take(2).joinToString(", ") { it.name },
                    severity = AlertSeverity.MEDIUM,
                    chapterId = staleChapters.first().id
                )
            )
        }
        val forgottenTopicIds = sessions.filter { it.outcomeEnum == RevisionOutcome.FORGOT }
            .groupBy { it.topicName.ifBlank { it.chapterName } }
            .filter { it.value.size >= 2 }
        if (forgottenTopicIds.isNotEmpty()) {
            val top = forgottenTopicIds.entries.first()
            alerts.add(
                AlertItem(
                    id = "alert_forgot",
                    title = "Repeatedly Forgotten: ${top.key}",
                    subtitle = "Marked Forgot ${top.value.size} times — schedule active re-learning",
                    severity = AlertSeverity.HIGH,
                    chapterId = top.value.first().chapterId
                )
            )
        }
        val upcomingTests = calendarEvents.filter {
            it.eventTypeEnum == CalendarEventType.ALLEN_TEST &&
                !it.isCompleted &&
                IstDateUtils.daysBetween(todayDate, it.date) in 0..7
        }
        upcomingTests.forEach { test ->
            val daysLeft = IstDateUtils.daysBetween(todayDate, test.date)
            alerts.add(
                AlertItem(
                    id = "alert_test_${test.id}",
                    title = "Upcoming Test: ${test.title}",
                    subtitle = if (daysLeft == 0) "Today at ${test.timeSlot}" else "In $daysLeft day${if (daysLeft == 1) "" else "s"} (${IstDateUtils.formatShort(test.date)})",
                    severity = AlertSeverity.INFO
                )
            )
        }

        // Real Retention Trend (last 7 session dates if any)
        val trendPoints = sessions
            .sortedBy { it.sessionDate }
            .groupBy { it.sessionDate }
            .entries
            .toList()
            .takeLast(7)
            .map { (date, daySessions) ->
                val score = daySessions.map {
                    when (it.outcomeEnum) {
                        RevisionOutcome.EASY -> 92.0
                        RevisionOutcome.GOOD -> 78.0
                        RevisionOutcome.DIFFICULT -> 54.0
                        RevisionOutcome.FORGOT -> 30.0
                    }
                }.average()
                IstDateUtils.formatShort(date) to score
            }

        return DashboardMetrics(
            dueTodayCount = dueTodayItems.size,
            completedTodayCount = completedToday,
            remainingTodayCount = overdueItems.size + dueTodayItems.size,
            overdueCount = overdueItems.size,
            upcoming7DaysCount = upcoming7Days,
            estimatedStudyMinutes = estimatedMinutes,
            weeklyCompletionRate = weeklyRate,
            monthlyCompletionRate = monthlyRate,
            revisionStreakDays = streak,
            strongChaptersCount = strongCount,
            weakChaptersCount = weakCount,
            overallEstimatedRetention = overallRetention,
            subjectSummaries = subjectSummaries,
            alerts = alerts,
            retentionTrendPoints = trendPoints
        )
    }

    /**
     * Generates deterministic, real-data recommendations (no invented assumptions).
     */
    fun generateRecommendations(
        chapters: List<Chapter>,
        topics: List<Topic>,
        sessions: List<RevisionSession>,
        errors: List<ErrorRecord>,
        testRecords: List<TestRecord>
    ): List<RecommendationItem> {
        val recs = mutableListOf<RecommendationItem>()
        val activeChapters = chapters.filter { !it.isArchived }
        val chapterMap = activeChapters.associateBy { it.id }

        // 1. Repeatedly forgotten topics from actual sessions
        sessions.filter { it.outcomeEnum == RevisionOutcome.FORGOT || it.outcomeEnum == RevisionOutcome.DIFFICULT }
            .groupBy { it.chapterId to it.topicId }
            .entries
            .sortedByDescending { it.value.size }
            .take(3)
            .forEach { (key, list) ->
                val sample = list.first()
                val avgAcc = list.filter { it.questionsAttempted > 0 }
                    .let { qs -> if (qs.isNotEmpty()) qs.sumOf { it.questionsCorrect } * 100.0 / qs.sumOf { it.questionsAttempted } else null }
                val targetLabel = if (sample.topicName.isNotBlank()) {
                    "${sample.topicName} in ${sample.chapterName}"
                } else {
                    sample.chapterName
                }
                val accText = avgAcc?.let { " with ${it.roundToInt()}% problem accuracy" } ?: ""
                val mistakeHint = list.firstOrNull { it.unclearConcept.isNotBlank() }?.unclearConcept
                    ?: list.firstOrNull { it.needsPractice.isNotBlank() }?.needsPractice
                    ?: "core derivations and application problems"
                recs.add(
                    RecommendationItem(
                        id = "rec_sess_${key.first}_${key.second}",
                        subject = sample.subjectEnum,
                        chapterId = sample.chapterId,
                        topicId = sample.topicId.ifBlank { null },
                        title = "Reinforce $targetLabel",
                        message = "You logged ${list.size} Difficult/Forgot session(s) on $targetLabel$accText. Consider revisiting $mistakeHint and solving targeted practice sets.",
                        actionLabel = "Revise Now"
                    )
                )
            }

        // 2. Weak chapters from Test Analysis
        testRecords.flatMap { test ->
            test.parsedChapterResults().filter { it.weakFlag || it.accuracyPercent < 60.0 }.map { test.testName to it }
        }.take(2).forEach { (testName, res) ->
            val ch = chapterMap[res.chapterId]
            if (ch != null) {
                recs.add(
                    RecommendationItem(
                        id = "rec_test_${ch.id}",
                        subject = ch.subjectEnum,
                        chapterId = ch.id,
                        topicId = null,
                        title = "Test Gap: ${ch.name}",
                        message = "Scored ${res.accuracyPercent.roundToInt()}% accuracy in $testName. Schedule an active recall + PYQ error review session.",
                        actionLabel = "Open Chapter"
                    )
                )
            }
        }

        // 3. Pending Errors clustered in a chapter
        errors.filter { it.reattemptStatus != "SOLVED_CORRECTLY" }
            .groupBy { it.chapterId }
            .entries
            .filter { it.value.size >= 2 }
            .take(2)
            .forEach { (chId, errList) ->
                val first = errList.first()
                recs.add(
                    RecommendationItem(
                        id = "rec_err_$chId",
                        subject = first.subjectEnum,
                        chapterId = chId,
                        topicId = null,
                        title = "${errList.size} Unresolved Errors in ${first.chapterName}",
                        message = "Most frequent category: ${errList.groupingBy { it.categoryEnum }.eachCount().maxByOrNull { it.value }?.key?.label ?: "Conceptual"}. Reattempt these before advancing.",
                        actionLabel = "Review Chapter"
                    )
                )
            }

        // 4. Topics marked Needs Attention
        topics.filter { !it.isArchived && it.statusEnum == ChapterStatus.NEEDS_ATTENTION }
            .take(2)
            .forEach { topic ->
                val chName = chapterMap[topic.chapterId]?.name ?: ""
                if (recs.none { it.topicId == topic.id }) {
                    recs.add(
                        RecommendationItem(
                            id = "rec_topic_${topic.id}",
                            subject = topic.subjectEnum,
                            chapterId = topic.chapterId,
                            topicId = topic.id,
                            title = "Topic Attention: ${topic.name}",
                            message = "Part of $chName is currently flagged as Needs Attention. Run a focused 20-minute topic revision.",
                            actionLabel = "Revise Topic"
                        )
                    )
                }
            }

        return recs.distinctBy { it.id }
    }

    /**
     * Generates Sunday Weekly Report from actual sessions, chapters, and errors.
     */
    fun generateWeeklyReport(
        chapters: List<Chapter>,
        sessions: List<RevisionSession>,
        errors: List<ErrorRecord>,
        todayDate: String = IstDateUtils.todayIst()
    ): WeeklyReport {
        val weekStart = IstDateUtils.addDays(todayDate, -6)
        val weekSessions = sessions.filter { it.sessionDate in weekStart..todayDate }
        val activeChapters = chapters.filter { !it.isArchived }
        val missed = activeChapters.count {
            it.nextRevisionDate.isNotBlank() && it.nextRevisionDate in weekStart..todayDate
        }
        val scheduledTotal = weekSessions.size + missed

        val subjectMap = Subject.entries.associateWith { s ->
            weekSessions.count { it.subjectEnum == s }
        }
        val strong = activeChapters.filter { it.statusEnum == ChapterStatus.STRONG }.map { it.name }
        val weak = activeChapters.filter { it.statusEnum == ChapterStatus.NEEDS_ATTENTION }.map { it.name }
        val repeatedErrors = errors.count { it.dateRecorded in weekStart..todayDate }
        val totalQ = weekSessions.sumOf { it.questionsAttempted }
        val correctQ = weekSessions.sumOf { it.questionsCorrect }
        val avgAcc = if (totalQ > 0) (correctQ * 100.0) / totalQ else null

        val nextWeekEnd = IstDateUtils.addDays(todayDate, 7)
        val nextWeekLoad = activeChapters.count {
            it.nextRevisionDate.isNotBlank() && it.nextRevisionDate in todayDate..nextWeekEnd
        }

        val plan = mutableListOf<String>()
        if (weak.isNotEmpty()) {
            plan.add("Prioritize weak chapters first: ${weak.take(3).joinToString(", ")}.")
        }
        if (missed > 0) {
            plan.add("Clear $missed overdue/missed chapter revision(s) in the first two days of the week.")
        }
        if (repeatedErrors > 0) {
            plan.add("Reattempt $repeatedErrors newly logged error notebook entries.")
        }
        if (plan.isEmpty()) {
            plan.add(
                if (weekSessions.isEmpty()) "Mark your currently studying Class 11 chapters to launch your spaced-repetition schedule."
                else "Maintain your daily revision streak and keep problem accuracy above 75%."
            )
        }

        return WeeklyReport(
            weekStartDate = weekStart,
            weekEndDate = todayDate,
            revisionsScheduled = scheduledTotal,
            revisionsCompleted = weekSessions.size,
            revisionsMissed = missed,
            subjectCompletionMap = subjectMap,
            strongChapters = strong,
            weakChapters = weak,
            repeatedErrorsCount = repeatedErrors,
            averageAccuracy = avgAcc,
            nextWeekWorkloadCount = nextWeekLoad,
            suggestedActionPlan = plan
        )
    }

    fun generateMonthlyReport(
        chapters: List<Chapter>,
        sessions: List<RevisionSession>,
        errors: List<ErrorRecord>,
        todayDate: String = IstDateUtils.todayIst()
    ): MonthlyReport {
        val monthStart = IstDateUtils.addDays(todayDate, -29)
        val monthSessions = sessions.filter { it.sessionDate in monthStart..todayDate }
        val activeChapters = chapters.filter { !it.isArchived }
        val totalQ = monthSessions.sumOf { it.questionsAttempted }
        val correctQ = monthSessions.sumOf { it.questionsCorrect }
        val avgAcc = if (totalQ > 0) (correctQ * 100.0) / totalQ else null
        val (year, month) = IstDateUtils.currentYearMonth(todayDate)

        val subjectRetention = Subject.entries.associateWith { subject ->
            val studied = activeChapters.filter {
                it.subjectEnum == subject && (it.completedRevisions > 0 || it.estimatedRetention > 0)
            }
            if (studied.isEmpty()) null
            else studied.map {
                estimateCurrentRetention(
                    it.lastRevisionDate,
                    it.currentIntervalDays.toInt(),
                    it.estimatedRetention,
                    it.statusEnum,
                    todayDate
                )
            }.average()
        }

        val topMistake = errors.filter { it.dateRecorded in monthStart..todayDate }
            .groupingBy { it.categoryEnum }
            .eachCount()
            .maxByOrNull { it.value }?.key

        return MonthlyReport(
            monthLabel = IstDateUtils.monthYearLabel(year, month),
            totalSessions = monthSessions.size,
            totalQuestionsSolved = totalQ,
            averageAccuracy = avgAcc,
            strongChaptersCount = activeChapters.count { it.statusEnum == ChapterStatus.STRONG },
            activeChaptersCount = activeChapters.count { it.statusEnum != ChapterStatus.NOT_STARTED },
            subjectRetentionMap = subjectRetention,
            topMistakeCategory = topMistake
        )
    }

    /**
     * Auto-balances flexible future revisions across nearby available days when any day
     * exceeds dailyRevisionCapacity. Never deletes or drops overdue revisions.
     */
    fun computeLoadBalancedMoves(
        chapters: List<Chapter>,
        dailyCapacity: Int,
        blockedDates: Set<String>,
        todayDate: String = IstDateUtils.todayIst()
    ): List<Pair<Chapter, String>> {
        val moves = mutableListOf<Pair<Chapter, String>>()
        val futureChapters = chapters.filter {
            !it.isArchived &&
                it.nextRevisionDate.isNotBlank() &&
                IstDateUtils.daysBetween(todayDate, it.nextRevisionDate) >= 0
        }.sortedBy { it.nextRevisionDate }

        val counts = mutableMapOf<String, Int>()
        for (ch in futureChapters) {
            val currentCount = counts[ch.nextRevisionDate] ?: 0
            if (blockedDates.contains(ch.nextRevisionDate) || currentCount >= dailyCapacity) {
                var target = ch.nextRevisionDate
                for (offset in 1..21) {
                    val candidate = IstDateUtils.addDays(ch.nextRevisionDate, offset)
                    if (!blockedDates.contains(candidate) && (counts[candidate] ?: 0) < dailyCapacity) {
                        target = candidate
                        break
                    }
                }
                if (target != ch.nextRevisionDate) {
                    moves.add(ch to target)
                    counts[target] = (counts[target] ?: 0) + 1
                } else {
                    counts[ch.nextRevisionDate] = currentCount + 1
                }
            } else {
                counts[ch.nextRevisionDate] = currentCount + 1
            }
        }
        return moves
    }

    /**
     * Evaluates a user's answer for MCQ, Numerical, or Short Answer practice questions.
     */
    fun evaluatePracticeAnswer(
        question: PracticeQuestion,
        rawUserAnswer: String
    ): EvaluatedQuestionResult {
        val userClean = rawUserAnswer.trim()
        val expectedClean = question.correctAnswer.trim()

        if (userClean.isBlank()) {
            return EvaluatedQuestionResult(
                question = question,
                userAnswer = "",
                isCorrect = false,
                feedback = "Unanswered. Review the solution steps below."
            )
        }

        val isCorrect = when (question.type) {
            PracticeQuestionType.MCQ -> {
                userClean.equals(expectedClean, ignoreCase = true)
            }

            PracticeQuestionType.NUMERICAL -> {
                val userNum = extractLeadingNumber(userClean)
                val expectedNum = extractLeadingNumber(expectedClean)
                if (userNum != null && expectedNum != null) {
                    val tol = max(question.numericalTolerance, abs(expectedNum) * 0.025)
                    abs(userNum - expectedNum) <= tol
                } else {
                    normalizeAnswerText(userClean) == normalizeAnswerText(expectedClean)
                }
            }

            PracticeQuestionType.SHORT_ANSWER -> {
                val normUser = normalizeAnswerText(userClean)
                val normExpected = normalizeAnswerText(expectedClean)
                if (normUser == normExpected ||
                    (normUser.length >= 4 && (normExpected.contains(normUser) || normUser.contains(normExpected)))
                ) {
                    true
                } else {
                    val expectedTokens = tokenizeMeaningfulWords(expectedClean)
                    val userTokens = tokenizeMeaningfulWords(userClean)
                    if (expectedTokens.isEmpty()) false
                    else {
                        val overlap = expectedTokens.count { it in userTokens }
                        overlap.toDouble() / expectedTokens.size.toDouble() >= 0.6
                    }
                }
            }
        }

        val feedback = if (isCorrect) {
            "Correct"
        } else {
            "Expected: ${question.correctAnswer}"
        }
        return EvaluatedQuestionResult(
            question = question,
            userAnswer = userClean,
            isCorrect = isCorrect,
            feedback = feedback
        )
    }

    private fun extractLeadingNumber(raw: String): Double? {
        val match = Regex("""[-+]?\d*\.?\d+""").find(raw.replace(",", ""))
        return match?.value?.toDoubleOrNull()
    }

    private fun normalizeAnswerText(raw: String): String =
        raw.lowercase()
            .replace(Regex("""[^a-z0-9.=+\-/*^]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun tokenizeMeaningfulWords(raw: String): Set<String> {
        val stopWords = setOf("the", "is", "are", "of", "and", "or", "to", "in", "a", "an", "for", "with", "that", "this")
        return normalizeAnswerText(raw)
            .split(" ")
            .map { it.trim() }
            .filter { it.length >= 2 && it !in stopWords }
            .toSet()
    }

    /**
     * Converts chapter/topic practice test results into updated spaced repetition scheduling,
     * pulling revision dates earlier when performance is weak (< 70%).
     */
    fun computeChapterTestRevisionImpact(
        chapter: Chapter,
        topic: Topic?,
        results: List<EvaluatedQuestionResult>,
        customIntervals: List<Int>,
        todayDate: String = IstDateUtils.todayIst(),
        dailyCapacity: Int = 6,
        scheduledCountsByDate: Map<String, Int> = emptyMap(),
        blockedDates: Set<String> = emptySet()
    ): ChapterTestScheduleImpact {
        val total = results.size.coerceAtLeast(1)
        val correct = results.count { it.isCorrect }
        val accuracy = (correct * 100.0) / total

        val (outcome, understanding, confidence, difficulty) = when {
            accuracy < 45.0 -> listOf(RevisionOutcome.FORGOT, 2, 2, 4)
            accuracy < 70.0 -> listOf(RevisionOutcome.DIFFICULT, 3, 3, 4)
            accuracy < 85.0 -> listOf(RevisionOutcome.GOOD, 4, 4, 3)
            else -> listOf(RevisionOutcome.EASY, 5, 5, 2)
        }

        val currentInterval = (topic?.currentIntervalDays ?: chapter.currentIntervalDays).toInt()
        val currentSuccesses = (topic?.consecutiveSuccesses ?: chapter.consecutiveSuccesses).toInt()
        val existingNextDate = (topic?.nextRevisionDate ?: chapter.nextRevisionDate)

        val baseSchedule = computeNextSchedule(
            currentIntervalDays = currentInterval,
            consecutiveSuccesses = currentSuccesses,
            outcome = outcome as RevisionOutcome,
            understanding = understanding as Int,
            confidence = confidence as Int,
            difficulty = difficulty as Int,
            questionsAttempted = total,
            questionsCorrect = correct,
            questionsWithHints = 0,
            questionsUnsolved = total - correct,
            timeSpentMinutes = total * 2,
            previousOutcomes = emptyList(),
            customIntervals = customIntervals,
            todayDate = todayDate,
            dailyCapacity = dailyCapacity,
            scheduledCountsByDate = scheduledCountsByDate,
            blockedDates = blockedDates,
            cumulativeChapterQuestions = chapter.totalQuestions.toInt()
        )

        val targetLabel = topic?.name ?: chapter.name
        val isPoor = accuracy < 70.0
        val wasScheduledLater = existingNextDate.isNotBlank() &&
            IstDateUtils.daysBetween(baseSchedule.nextRevisionDate, existingNextDate) > 0
        val triggeredEarlier = isPoor && (existingNextDate.isBlank() || wasScheduledLater)

        val finalStatus = if (accuracy < 60.0) {
            ChapterStatus.NEEDS_ATTENTION
        } else {
            baseSchedule.newStatus
        }

        val finalNextDate = if (isPoor && existingNextDate.isNotBlank() && !wasScheduledLater) {
            // Already due today/overdue or sooner than computed date: keep sooner urgent date
            existingNextDate
        } else {
            baseSchedule.nextRevisionDate
        }

        val finalExplanation = buildString {
            append("Practice Test ($targetLabel): $correct/$total (${accuracy.roundToInt()}%). ")
            if (triggeredEarlier) {
                append("Low accuracy triggered an earlier revision on ${IstDateUtils.formatShort(finalNextDate)}.")
            } else if (isPoor) {
                append("Flagged for reinforcement on ${IstDateUtils.formatShort(finalNextDate)}.")
            } else {
                append("Next review set for ${IstDateUtils.formatShort(finalNextDate)} (${baseSchedule.nextIntervalDays}d interval).")
            }
        }

        val adjustedSchedule = baseSchedule.copy(
            nextRevisionDate = finalNextDate,
            newStatus = finalStatus,
            explanation = finalExplanation
        )

        val weakTags = results
            .filter { !it.isCorrect }
            .map { it.question.conceptTag }
            .distinct()

        val testChapterResult = TestChapterResult(
            chapterId = chapter.id,
            chapterName = if (topic != null) "${chapter.name} • ${topic.name}" else chapter.name,
            subject = chapter.subject,
            accuracyPercent = accuracy,
            questionsAttempted = total.toLong(),
            questionsCorrect = correct.toLong(),
            weakFlag = accuracy < 60.0
        )

        return ChapterTestScheduleImpact(
            scheduleResult = adjustedSchedule,
            triggeredEarlierRevision = triggeredEarlier,
            weakConceptTags = weakTags,
            testChapterResult = testChapterResult,
            accuracyPercent = accuracy
        )
    }
}
