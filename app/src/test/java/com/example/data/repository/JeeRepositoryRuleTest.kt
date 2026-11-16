package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.firestore.JeeRepository
import com.example.domain.engine.IstDateUtils
import com.example.domain.engine.SpacedRepetitionEngine
import com.example.domain.model.Chapter
import com.example.domain.model.ChapterStatus
import com.example.domain.model.ClassLevel
import com.example.domain.model.ErrorCategory
import com.example.domain.model.ErrorRecord
import com.example.domain.model.PracticeDifficulty
import com.example.domain.model.PracticeQuestion
import com.example.domain.model.PracticeQuestionType
import com.example.domain.model.RevisionOutcome
import com.example.domain.model.RevisionSession
import com.example.domain.model.Subject
import com.example.domain.model.TestType
import com.example.domain.model.Topic
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class JeeRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun chapterCreation_revisionScheduling_completion_and_history_endToEnd() = runBlocking {
        val uid = signInTestUser("aspirant_${UUID.randomUUID().toString().take(6)}@test.com")
        val repo = JeeRepository(firestore, auth)

        val chapterId = "ch_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val chapter = Chapter(
            id = chapterId,
            userId = uid,
            name = "Laws of Motion",
            subject = Subject.PHYSICS.name,
            classLevel = ClassLevel.CLASS_11.name,
            status = ChapterStatus.THEORY_COMPLETED.name,
            initialLearningDate = IstDateUtils.todayIst(),
            nextRevisionDate = IstDateUtils.todayIst(),
            originalDueDate = IstDateUtils.todayIst(),
            currentIntervalDays = 1L,
            importance = 5L
        )
        withTimeout(TIMEOUT_MS) { repo.createChapter(chapter) }

        val topicId = "tp_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val topic = Topic(
            id = topicId,
            userId = uid,
            chapterId = chapterId,
            subject = Subject.PHYSICS.name,
            name = "Friction",
            status = ChapterStatus.THEORY_COMPLETED.name,
            nextRevisionDate = IstDateUtils.todayIst(),
            originalDueDate = IstDateUtils.todayIst(),
            currentIntervalDays = 1L
        )
        withTimeout(TIMEOUT_MS) { repo.createTopic(topic) }

        val schedule = SpacedRepetitionEngine.computeNextSchedule(
            currentIntervalDays = 1,
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
            customIntervals = listOf(1, 3, 7, 14, 30, 60, 90)
        )

        val session = RevisionSession(
            userId = uid,
            chapterId = chapterId,
            chapterName = chapter.name,
            topicId = topicId,
            topicName = topic.name,
            subject = Subject.PHYSICS.name,
            sessionDate = IstDateUtils.todayIst(),
            durationMinutes = 25L,
            activeRecallNotes = "Recalled static and kinetic friction laws",
            theoryChecklist = "Definitions,Formulas",
            questionsAttempted = 10L,
            questionsCorrect = 8L,
            questionsIncorrect = 1L,
            questionsWithHints = 1L,
            questionsUnsolved = 1L,
            whatForgot = "Angle of friction relation",
            mistakeMade = "Sign error on incline",
            understandingRating = 4L,
            confidenceRating = 4L,
            difficultyRating = 3L,
            outcome = RevisionOutcome.GOOD.name,
            previousIntervalDays = 1L
        )

        val err = ErrorRecord(
            userId = uid,
            subject = Subject.PHYSICS.name,
            chapterId = chapterId,
            chapterName = chapter.name,
            topicId = topicId,
            topicName = topic.name,
            questionRef = "HCV Friction Q18",
            errorCategory = ErrorCategory.CONCEPTUAL.name,
            mistakeExplanation = "Assumed limiting friction without checking motion",
            correctApproach = "Compare required f with mu_s * N",
            importantLesson = "Static friction is self-adjusting",
            dateRecorded = IstDateUtils.todayIst()
        )

        val sessionId = withTimeout(TIMEOUT_MS) {
            repo.recordRevisionSession(session, chapter, topic, schedule, err)
        }
        assertTrue(sessionId.isNotBlank())

        val chapters = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeChapters(uid).first { list -> list.any { it.id == chapterId } }
        }
        val savedChapter = chapters.first { it.id == chapterId }
        assertEquals(10L, savedChapter.totalQuestions)
        assertEquals(8L, savedChapter.correctQuestions)

        val topics = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeTopics(uid).first { list -> list.any { it.id == topicId && it.completedRevisions == 1L } }
        }
        val savedTopic = topics.first { it.id == topicId }
        assertEquals(schedule.nextRevisionDate, savedTopic.nextRevisionDate)
        assertTrue(savedTopic.lastRescheduleReason.isNotBlank())

        val history = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeRevisionSessions(uid).first { list -> list.any { it.id == sessionId } }
        }
        assertEquals(1, history.count { it.id == sessionId })
    }

    @Test
    fun observeChapters_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = JeeRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeChapters("some_user_123").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated caller")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    @Test
    fun crossUserReadAndWrite_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_rule@test.com")
        val aliceRepo = JeeRepository(firestore, auth)
        val chId = "ch_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        withTimeout(TIMEOUT_MS) {
            aliceRepo.createChapter(
                Chapter(
                    id = chId,
                    userId = aliceUid,
                    name = "Rotational Motion",
                    subject = Subject.PHYSICS.name
                )
            )
        }

        signInTestUser("bob_rule@test.com")
        val bobRepo = JeeRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeChapters(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED on cross-user read")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    @Test
    fun recordChapterPracticeTest_poorPerformance_triggersEarlierRevisionAndUpdatesMetrics() = runBlocking {
        val uid = signInTestUser("practice_${UUID.randomUUID().toString().take(6)}@test.com")
        val repo = JeeRepository(firestore, auth)

        val today = IstDateUtils.todayIst()
        val futureDue = IstDateUtils.addDays(today, 20)

        val chapterId = "ch_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val chapter = Chapter(
            id = chapterId,
            userId = uid,
            name = "Electrostatics",
            subject = Subject.PHYSICS.name,
            classLevel = ClassLevel.CLASS_12.name,
            status = ChapterStatus.REVISION_IN_PROGRESS.name,
            initialLearningDate = IstDateUtils.addDays(today, -14),
            nextRevisionDate = futureDue,
            originalDueDate = futureDue,
            currentIntervalDays = 14L
        )
        withTimeout(TIMEOUT_MS) { repo.createChapter(chapter) }

        val topicId = "tp_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val topic = Topic(
            id = topicId,
            userId = uid,
            chapterId = chapterId,
            subject = Subject.PHYSICS.name,
            name = "Gauss Law",
            status = ChapterStatus.REVISION_IN_PROGRESS.name,
            initialLearningDate = IstDateUtils.addDays(today, -14),
            nextRevisionDate = futureDue,
            originalDueDate = futureDue,
            currentIntervalDays = 14L
        )
        withTimeout(TIMEOUT_MS) { repo.createTopic(topic) }

        val q1 = PracticeQuestion("q1", PracticeQuestionType.MCQ, PracticeDifficulty.MEDIUM, "Flux", "P1", listOf("A", "B"), "A", 0.02, "S1")
        val q2 = PracticeQuestion("q2", PracticeQuestionType.NUMERICAL, PracticeDifficulty.HARD, "Sphere Field", "P2", emptyList(), "4.0", 0.05, "S2")
        val q3 = PracticeQuestion("q3", PracticeQuestionType.SHORT_ANSWER, PracticeDifficulty.EASY, "Symmetry", "P3", emptyList(), "Gaussian surface", 0.02, "S3")

        val results = listOf(
            SpacedRepetitionEngine.evaluatePracticeAnswer(q1, "B"),
            SpacedRepetitionEngine.evaluatePracticeAnswer(q2, "9.0"),
            SpacedRepetitionEngine.evaluatePracticeAnswer(q3, "Gaussian surface")
        )
        val impact = SpacedRepetitionEngine.computeChapterTestRevisionImpact(
            chapter = chapter,
            topic = topic,
            results = results,
            customIntervals = listOf(1, 3, 7, 14, 30, 60, 90),
            todayDate = today
        )
        assertTrue(impact.triggeredEarlierRevision)

        val testId = withTimeout(TIMEOUT_MS) {
            repo.recordChapterPracticeTest(
                chapter = chapter,
                topic = topic,
                impact = impact,
                durationMinutes = 8,
                testTitle = "Practice Test: Gauss Law"
            )
        }
        assertTrue(testId.isNotBlank())

        val tests = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeTestRecords(uid).first { list -> list.any { it.id == testId } }
        }
        val savedTest = tests.first { it.id == testId }
        assertEquals(TestType.CHAPTER_TEST.name, savedTest.testType)
        assertEquals(1.0, savedTest.totalMarks, 0.01)
        assertEquals(3.0, savedTest.maxMarks, 0.01)

        val savedTopic = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeTopics(uid).first { list -> list.any { it.id == topicId && it.totalQuestions == 3L } }
        }.first { it.id == topicId }
        assertEquals(ChapterStatus.NEEDS_ATTENTION, savedTopic.statusEnum)
        assertEquals(impact.scheduleResult.nextRevisionDate, savedTopic.nextRevisionDate)
        assertTrue(IstDateUtils.daysBetween(savedTopic.nextRevisionDate, futureDue) > 0)

        val savedChapter = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeChapters(uid).first { list -> list.any { it.id == chapterId && it.totalQuestions == 3L } }
        }.first { it.id == chapterId }
        assertEquals(1L, savedChapter.correctQuestions)
        assertEquals(2L, savedChapter.incorrectQuestions)
    }

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
