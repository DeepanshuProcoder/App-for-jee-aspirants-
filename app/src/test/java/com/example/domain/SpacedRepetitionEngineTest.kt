package com.example.domain

import com.example.domain.engine.BackupSerializer
import com.example.domain.engine.IstDateUtils
import com.example.domain.engine.JeeSyllabusCatalog
import com.example.domain.engine.SpacedRepetitionEngine
import com.example.domain.model.Chapter
import com.example.domain.model.ChapterStatus
import com.example.domain.model.ChemistrySection
import com.example.domain.model.ClassLevel
import com.example.domain.model.PracticeDifficulty
import com.example.domain.model.PracticeQuestion
import com.example.domain.model.PracticeQuestionType
import com.example.domain.model.QueueCategory
import com.example.domain.model.RevisionOutcome
import com.example.domain.model.Subject
import com.example.domain.model.Topic
import com.example.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SpacedRepetitionEngineTest {

    @Test
    fun `easy outcome extends interval moderately with explanation`() {
        val res = SpacedRepetitionEngine.computeNextSchedule(
            currentIntervalDays = 7,
            consecutiveSuccesses = 2,
            outcome = RevisionOutcome.EASY,
            understanding = 5,
            confidence = 5,
            difficulty = 2,
            questionsAttempted = 20,
            questionsCorrect = 18,
            questionsWithHints = 0,
            questionsUnsolved = 1,
            timeSpentMinutes = 35,
            previousOutcomes = listOf(RevisionOutcome.GOOD, RevisionOutcome.EASY),
            customIntervals = listOf(1, 3, 7, 14, 30, 60, 90),
            todayDate = "2026-08-01",
            dailyCapacity = 6,
            cumulativeChapterQuestions = 20
        )
        assertTrue(res.nextIntervalDays > 14)
        assertEquals(3, res.newConsecutiveSuccesses)
        assertEquals(ChapterStatus.STRONG, res.newStatus)
        assertTrue(res.explanation.contains("EASY"))
    }

    @Test
    fun `strong status is gated behind real recall and problem solving evidence`() {
        val res = SpacedRepetitionEngine.computeNextSchedule(
            currentIntervalDays = 7,
            consecutiveSuccesses = 2,
            outcome = RevisionOutcome.EASY,
            understanding = 5,
            confidence = 5,
            difficulty = 2,
            questionsAttempted = 2,
            questionsCorrect = 2,
            questionsWithHints = 0,
            questionsUnsolved = 0,
            timeSpentMinutes = 15,
            previousOutcomes = emptyList(),
            customIntervals = listOf(1, 3, 7, 14, 30, 60, 90),
            todayDate = "2026-08-01",
            cumulativeChapterQuestions = 3 // total = 5 < 10 required
        )
        assertEquals(ChapterStatus.REVISION_IN_PROGRESS, res.newStatus)
    }

    @Test
    fun `forgot outcome restarts review sequence and marks needs attention`() {
        val res = SpacedRepetitionEngine.computeNextSchedule(
            currentIntervalDays = 30,
            consecutiveSuccesses = 4,
            outcome = RevisionOutcome.FORGOT,
            understanding = 2,
            confidence = 1,
            difficulty = 5,
            questionsAttempted = 10,
            questionsCorrect = 2,
            questionsWithHints = 5,
            questionsUnsolved = 5,
            timeSpentMinutes = 50,
            previousOutcomes = listOf(RevisionOutcome.DIFFICULT),
            customIntervals = listOf(1, 3, 7, 14, 30, 60, 90),
            todayDate = "2026-08-01"
        )
        assertEquals(1, res.nextIntervalDays)
        assertEquals(0, res.newConsecutiveSuccesses)
        assertEquals(ChapterStatus.NEEDS_ATTENTION, res.newStatus)
        assertEquals("2026-08-02", res.nextRevisionDate)
    }

    @Test
    fun `blocked dates and full capacity days load balance future schedule`() {
        val res = SpacedRepetitionEngine.computeNextSchedule(
            currentIntervalDays = 3,
            consecutiveSuccesses = 1,
            outcome = RevisionOutcome.GOOD,
            understanding = 4,
            confidence = 4,
            difficulty = 3,
            questionsAttempted = 15,
            questionsCorrect = 12,
            questionsWithHints = 1,
            questionsUnsolved = 1,
            timeSpentMinutes = 30,
            previousOutcomes = emptyList(),
            customIntervals = listOf(1, 3, 7, 14, 30, 60, 90),
            todayDate = "2026-08-01",
            dailyCapacity = 4,
            scheduledCountsByDate = mapOf("2026-08-08" to 4),
            blockedDates = setOf("2026-08-09")
        )
        assertTrue(res.wasLoadBalancedOrShifted)
        assertFalse(res.nextRevisionDate == "2026-08-08")
        assertFalse(res.nextRevisionDate == "2026-08-09")
        assertEquals("2026-08-10", res.nextRevisionDate)
    }

    @Test
    fun `empty workspace yields zero fake retention or completion metrics`() {
        val chapters = JeeSyllabusCatalog.createSeedChapters("u1")
        val metrics = SpacedRepetitionEngine.computeDashboardMetrics(
            chapters = chapters,
            topics = emptyList(),
            sessions = emptyList(),
            calendarEvents = emptyList(),
            todayDate = "2026-08-01"
        )
        assertEquals(0, metrics.completedTodayCount)
        assertEquals(0, metrics.dueTodayCount)
        assertEquals(0, metrics.overdueCount)
        assertNull(metrics.weeklyCompletionRate)
        assertNull(metrics.monthlyCompletionRate)
        assertNull(metrics.overallEstimatedRetention)
        assertTrue(metrics.retentionTrendPoints.isEmpty())
    }

    @Test
    fun `queue prioritizes overdue chapters ahead of upcoming previews`() {
        val overdue = Chapter(
            id = "ch1",
            userId = "u1",
            name = "Rotational Motion",
            subject = Subject.PHYSICS.name,
            status = ChapterStatus.REVISION_IN_PROGRESS.name,
            nextRevisionDate = "2026-07-29",
            originalDueDate = "2026-07-29",
            currentIntervalDays = 7
        )
        val upcoming = Chapter(
            id = "ch2",
            userId = "u1",
            name = "Mole Concept",
            subject = Subject.CHEMISTRY.name,
            section = ChemistrySection.PHYSICAL.firestoreValue,
            status = ChapterStatus.REVISION_IN_PROGRESS.name,
            nextRevisionDate = "2026-08-03",
            originalDueDate = "2026-08-03",
            currentIntervalDays = 14
        )
        val queue = SpacedRepetitionEngine.buildRevisionQueue(
            chapters = listOf(upcoming, overdue),
            topics = emptyList(),
            calendarEvents = emptyList(),
            todayDate = "2026-08-01",
            includeUpcoming = true
        )
        assertEquals(2, queue.size)
        assertEquals(QueueCategory.OVERDUE, queue.first().category)
        assertEquals("Rotational Motion", queue.first().chapterName)
    }

    @Test
    fun `ist date utils and backup serializer round trip accurately`() {
        assertEquals("2026-08-05", IstDateUtils.addDays("2026-08-01", 4))
        assertEquals(4, IstDateUtils.daysBetween("2026-08-01", "2026-08-05"))

        val ch = Chapter(
            id = "ch_rot",
            userId = "u1",
            name = "Rotational Motion",
            subject = Subject.PHYSICS.name,
            classLevel = ClassLevel.CLASS_11.name,
            status = ChapterStatus.CURRENTLY_STUDYING.name
        )
        val json = BackupSerializer.toJsonString(
            profile = UserProfile(userId = "u1", displayName = "Aarav"),
            chapters = listOf(ch),
            topics = emptyList(),
            sessions = emptyList(),
            events = emptyList(),
            tests = emptyList(),
            errors = emptyList(),
            notes = emptyList()
        )
        val restored = BackupSerializer.fromJsonString(json, "u1")
        assertEquals(1, restored.chapters.size)
        assertEquals("Rotational Motion", restored.chapters.first().name)
    }

    @Test
    fun `evaluatePracticeAnswer scores MCQ, numerical tolerance, and short answer keywords`() {
        val mcq = PracticeQuestion(
            id = "q1",
            type = PracticeQuestionType.MCQ,
            difficulty = PracticeDifficulty.MEDIUM,
            conceptTag = "Gauss Law",
            prompt = "Flux through a cube?",
            options = listOf("q/e0", "q/6e0", "q/2e0", "0"),
            correctAnswer = "q/6e0",
            solutionSteps = "Symmetry of 6 faces"
        )
        assertTrue(SpacedRepetitionEngine.evaluatePracticeAnswer(mcq, "q/6e0").isCorrect)
        assertFalse(SpacedRepetitionEngine.evaluatePracticeAnswer(mcq, "q/e0").isCorrect)

        val num = PracticeQuestion(
            id = "q2",
            type = PracticeQuestionType.NUMERICAL,
            difficulty = PracticeDifficulty.HARD,
            conceptTag = "Moment of Inertia",
            prompt = "Find k in kg m^2",
            correctAnswer = "9.8",
            numericalTolerance = 0.05,
            solutionSteps = "Integrate dm r^2"
        )
        assertTrue(SpacedRepetitionEngine.evaluatePracticeAnswer(num, "9.82 m^2").isCorrect)
        assertFalse(SpacedRepetitionEngine.evaluatePracticeAnswer(num, "11.0").isCorrect)

        val shortQ = PracticeQuestion(
            id = "q3",
            type = PracticeQuestionType.SHORT_ANSWER,
            difficulty = PracticeDifficulty.EASY,
            conceptTag = "Rolling Motion",
            prompt = "Condition for pure rolling?",
            correctAnswer = "v = omega R without slipping",
            solutionSteps = "Point of contact velocity is zero"
        )
        assertTrue(SpacedRepetitionEngine.evaluatePracticeAnswer(shortQ, "omega R with no slipping").isCorrect)
        assertFalse(SpacedRepetitionEngine.evaluatePracticeAnswer(shortQ, "").isCorrect)
    }

    @Test
    fun `computeChapterTestRevisionImpact pulls revision earlier on poor topic score and extends on strong score`() {
        val chapter = Chapter(
            id = "ch_rot",
            userId = "u1",
            name = "Rotational Motion",
            subject = Subject.PHYSICS.name,
            status = ChapterStatus.REVISION_IN_PROGRESS.name,
            currentIntervalDays = 14L,
            nextRevisionDate = "2026-08-20"
        )
        val topic = Topic(
            id = "tp_moi",
            userId = "u1",
            chapterId = "ch_rot",
            subject = Subject.PHYSICS.name,
            name = "Moment of Inertia",
            status = ChapterStatus.REVISION_IN_PROGRESS.name,
            currentIntervalDays = 14L,
            nextRevisionDate = "2026-08-20"
        )
        val q1 = PracticeQuestion("q1", PracticeQuestionType.MCQ, PracticeDifficulty.MEDIUM, "Parallel Axis", "P1", listOf("A", "B"), "A", 0.02, "S1")
        val q2 = PracticeQuestion("q2", PracticeQuestionType.NUMERICAL, PracticeDifficulty.HARD, "Perpendicular Axis", "P2", emptyList(), "5", 0.05, "S2")
        val q3 = PracticeQuestion("q3", PracticeQuestionType.MCQ, PracticeDifficulty.EASY, "Torque", "P3", listOf("X", "Y"), "X", 0.02, "S3")

        val poorResults = listOf(
            SpacedRepetitionEngine.evaluatePracticeAnswer(q1, "B"),
            SpacedRepetitionEngine.evaluatePracticeAnswer(q2, "9"),
            SpacedRepetitionEngine.evaluatePracticeAnswer(q3, "X")
        )
        val poorImpact = SpacedRepetitionEngine.computeChapterTestRevisionImpact(
            chapter = chapter,
            topic = topic,
            results = poorResults,
            customIntervals = listOf(1, 3, 7, 14, 30, 60, 90),
            todayDate = "2026-08-01"
        )
        assertTrue(poorImpact.triggeredEarlierRevision)
        assertEquals(ChapterStatus.NEEDS_ATTENTION, poorImpact.scheduleResult.newStatus)
        assertTrue(IstDateUtils.daysBetween(poorImpact.scheduleResult.nextRevisionDate, "2026-08-20") > 0)
        assertEquals(listOf("Parallel Axis", "Perpendicular Axis"), poorImpact.weakConceptTags)

        val strongResults = listOf(
            SpacedRepetitionEngine.evaluatePracticeAnswer(q1, "A"),
            SpacedRepetitionEngine.evaluatePracticeAnswer(q2, "5"),
            SpacedRepetitionEngine.evaluatePracticeAnswer(q3, "X")
        )
        val strongImpact = SpacedRepetitionEngine.computeChapterTestRevisionImpact(
            chapter = chapter,
            topic = topic,
            results = strongResults,
            customIntervals = listOf(1, 3, 7, 14, 30, 60, 90),
            todayDate = "2026-08-01"
        )
        assertFalse(strongImpact.triggeredEarlierRevision)
        assertTrue(strongImpact.accuracyPercent >= 99.0)
        assertTrue(strongImpact.scheduleResult.nextIntervalDays > 14)
    }

    @Test
    fun `queue aggregates overdue, today, and weak topics sorted by urgency and weakness`() {
        val strongToday = Chapter(
            id = "ch_strong_today",
            userId = "u1",
            name = "Units and Dimensions",
            subject = Subject.PHYSICS.name,
            status = ChapterStatus.REVISION_IN_PROGRESS.name,
            nextRevisionDate = "2026-08-01",
            lastRevisionDate = "2026-07-25",
            currentIntervalDays = 7,
            estimatedRetention = 88.0,
            importance = 3
        )
        val overdueWeak = Chapter(
            id = "ch_overdue_weak",
            userId = "u1",
            name = "Rotational Motion",
            subject = Subject.PHYSICS.name,
            status = ChapterStatus.NEEDS_ATTENTION.name,
            nextRevisionDate = "2026-07-29",
            lastRevisionDate = "2026-07-20",
            currentIntervalDays = 3,
            estimatedRetention = 32.0,
            importance = 5
        )
        val overdueMild = Chapter(
            id = "ch_overdue_mild",
            userId = "u1",
            name = "Mole Concept",
            subject = Subject.CHEMISTRY.name,
            status = ChapterStatus.REVISION_IN_PROGRESS.name,
            nextRevisionDate = "2026-07-31",
            lastRevisionDate = "2026-07-24",
            currentIntervalDays = 7,
            estimatedRetention = 82.0,
            importance = 3
        )
        val weakTopicNoDate = Topic(
            id = "tp_weak_blank",
            userId = "u1",
            chapterId = "ch_strong_today",
            subject = Subject.PHYSICS.name,
            name = "Vernier Calipers",
            status = ChapterStatus.NEEDS_ATTENTION.name,
            estimatedRetention = 35.0
        )

        val queue = SpacedRepetitionEngine.buildRevisionQueue(
            chapters = listOf(strongToday, overdueMild, overdueWeak),
            topics = listOf(weakTopicNoDate),
            calendarEvents = emptyList(),
            todayDate = "2026-08-01",
            includeUpcoming = true
        )

        assertEquals(4, queue.size)
        // Overdue + Needs Attention + low retention must be #1
        assertEquals("chapter_ch_overdue_weak", queue[0].key)
        assertEquals(QueueCategory.OVERDUE, queue[0].category)
        // Next must be weak topic or 1-day overdue ahead of strong today item
        assertEquals("chapter_ch_strong_today", queue.last().key)
        assertTrue(queue.any { it.key == "topic_tp_weak_blank" && it.category == QueueCategory.URGENT_WEAK })
    }

    @Test
    fun `good outcome follows default and custom revision sequence with streak increment`() {
        val custom = listOf(2, 5, 10, 21, 45)
        val firstGood = SpacedRepetitionEngine.computeNextSchedule(
            currentIntervalDays = 0,
            consecutiveSuccesses = 0,
            outcome = RevisionOutcome.GOOD,
            understanding = 4,
            confidence = 4,
            difficulty = 3,
            questionsAttempted = 10,
            questionsCorrect = 8,
            questionsWithHints = 1,
            questionsUnsolved = 1,
            timeSpentMinutes = 25,
            previousOutcomes = emptyList(),
            customIntervals = custom,
            todayDate = "2026-08-01"
        )
        assertEquals(2, firstGood.nextIntervalDays)
        assertEquals(1, firstGood.newConsecutiveSuccesses)
        assertEquals("2026-08-03", firstGood.nextRevisionDate)

        val secondGood = SpacedRepetitionEngine.computeNextSchedule(
            currentIntervalDays = 2,
            consecutiveSuccesses = 1,
            outcome = RevisionOutcome.GOOD,
            understanding = 4,
            confidence = 4,
            difficulty = 3,
            questionsAttempted = 10,
            questionsCorrect = 8,
            questionsWithHints = 1,
            questionsUnsolved = 1,
            timeSpentMinutes = 25,
            previousOutcomes = listOf(RevisionOutcome.GOOD),
            customIntervals = custom,
            todayDate = "2026-08-03"
        )
        assertEquals(5, secondGood.nextIntervalDays)
        assertEquals(2, secondGood.newConsecutiveSuccesses)
        assertEquals("2026-08-08", secondGood.nextRevisionDate)
    }

    @Test
    fun `difficult outcome pulls review earlier resets streak and assigns extra practice`() {
        val moderateDifficult = SpacedRepetitionEngine.computeNextSchedule(
            currentIntervalDays = 14,
            consecutiveSuccesses = 3,
            outcome = RevisionOutcome.DIFFICULT,
            understanding = 3,
            confidence = 3,
            difficulty = 4,
            questionsAttempted = 10,
            questionsCorrect = 6,
            questionsWithHints = 4,
            questionsUnsolved = 2,
            timeSpentMinutes = 40,
            previousOutcomes = listOf(RevisionOutcome.GOOD),
            customIntervals = listOf(1, 3, 7, 14, 30, 60, 90),
            todayDate = "2026-08-01"
        )
        assertEquals(3, moderateDifficult.nextIntervalDays)
        assertEquals(0, moderateDifficult.newConsecutiveSuccesses)
        assertTrue(moderateDifficult.recommendedPracticeQuestions >= 12)
        assertTrue(moderateDifficult.explanation.contains("DIFFICULT"))

        val repeatedDifficult = SpacedRepetitionEngine.computeNextSchedule(
            currentIntervalDays = 14,
            consecutiveSuccesses = 0,
            outcome = RevisionOutcome.DIFFICULT,
            understanding = 3,
            confidence = 3,
            difficulty = 4,
            questionsAttempted = 10,
            questionsCorrect = 6,
            questionsWithHints = 3,
            questionsUnsolved = 1,
            timeSpentMinutes = 35,
            previousOutcomes = listOf(RevisionOutcome.DIFFICULT),
            customIntervals = listOf(1, 3, 7, 14, 30, 60, 90),
            todayDate = "2026-08-01"
        )
        assertEquals(1, repeatedDifficult.nextIntervalDays)
        assertEquals(ChapterStatus.NEEDS_ATTENTION, repeatedDifficult.newStatus)
        assertEquals(20, repeatedDifficult.recommendedPracticeQuestions)
    }

    @Test
    fun `forgot outcome bypasses capacity deferral but avoids explicitly blocked dates`() {
        val custom = listOf(2, 5, 10, 21, 45)
        val unblockedFullDay = SpacedRepetitionEngine.computeNextSchedule(
            currentIntervalDays = 21,
            consecutiveSuccesses = 3,
            outcome = RevisionOutcome.FORGOT,
            understanding = 1,
            confidence = 1,
            difficulty = 5,
            questionsAttempted = 8,
            questionsCorrect = 1,
            questionsWithHints = 4,
            questionsUnsolved = 4,
            timeSpentMinutes = 45,
            previousOutcomes = listOf(RevisionOutcome.GOOD),
            customIntervals = custom,
            todayDate = "2026-08-01",
            dailyCapacity = 2,
            scheduledCountsByDate = mapOf("2026-08-03" to 5),
            blockedDates = emptySet()
        )
        assertEquals(2, unblockedFullDay.nextIntervalDays)
        assertEquals("2026-08-03", unblockedFullDay.nextRevisionDate)
        assertFalse(unblockedFullDay.wasLoadBalancedOrShifted)
        assertEquals(ChapterStatus.NEEDS_ATTENTION, unblockedFullDay.newStatus)

        val blockedResetDay = SpacedRepetitionEngine.computeNextSchedule(
            currentIntervalDays = 21,
            consecutiveSuccesses = 3,
            outcome = RevisionOutcome.FORGOT,
            understanding = 1,
            confidence = 1,
            difficulty = 5,
            questionsAttempted = 8,
            questionsCorrect = 1,
            questionsWithHints = 4,
            questionsUnsolved = 4,
            timeSpentMinutes = 45,
            previousOutcomes = listOf(RevisionOutcome.GOOD),
            customIntervals = custom,
            todayDate = "2026-08-01",
            dailyCapacity = 2,
            scheduledCountsByDate = emptyMap(),
            blockedDates = setOf("2026-08-03")
        )
        assertTrue(blockedResetDay.wasLoadBalancedOrShifted)
        assertEquals("2026-08-04", blockedResetDay.nextRevisionDate)
    }

    @Test
    fun `estimateCurrentRetention returns zero when unstarted and decays monotonically over time`() {
        assertEquals(
            0.0,
            SpacedRepetitionEngine.estimateCurrentRetention(
                lastRevisionDate = "",
                currentIntervalDays = 0,
                storedRetention = 0.0,
                status = ChapterStatus.NOT_STARTED,
                todayDate = "2026-08-01"
            ),
            0.001
        )
        val day0 = SpacedRepetitionEngine.estimateCurrentRetention(
            lastRevisionDate = "2026-08-01",
            currentIntervalDays = 7,
            storedRetention = 88.0,
            status = ChapterStatus.REVISION_IN_PROGRESS,
            todayDate = "2026-08-01"
        )
        val day5 = SpacedRepetitionEngine.estimateCurrentRetention(
            lastRevisionDate = "2026-08-01",
            currentIntervalDays = 7,
            storedRetention = 88.0,
            status = ChapterStatus.REVISION_IN_PROGRESS,
            todayDate = "2026-08-06"
        )
        val day15 = SpacedRepetitionEngine.estimateCurrentRetention(
            lastRevisionDate = "2026-08-01",
            currentIntervalDays = 7,
            storedRetention = 88.0,
            status = ChapterStatus.REVISION_IN_PROGRESS,
            todayDate = "2026-08-16"
        )
        assertTrue(day0 > day5 && day5 > day15)
        assertTrue(day15 >= 5.0)
    }
}
