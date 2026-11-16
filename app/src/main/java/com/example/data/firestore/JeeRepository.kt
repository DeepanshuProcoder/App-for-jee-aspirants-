package com.example.data.firestore

import android.content.Context
import com.example.R
import com.example.domain.engine.BackupBundle
import com.example.domain.engine.IstDateUtils
import com.example.domain.engine.JeeSyllabusCatalog
import com.example.domain.model.CalendarEvent
import com.example.domain.model.Chapter
import com.example.domain.model.ChapterStatus
import com.example.domain.model.ChapterTestScheduleImpact
import com.example.domain.model.ErrorRecord
import com.example.domain.model.FormulaNote
import com.example.domain.model.RevisionSession
import com.example.domain.model.ScheduleComputationResult
import com.example.domain.model.Subject
import com.example.domain.model.TestRecord
import com.example.domain.model.TestType
import com.example.domain.model.Topic
import com.example.domain.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class JeeRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        userDoc(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else null
            }
            .catch { error ->
                logFlowError(error, OperationType.GET, "users/$userId", userId)
                throw error
            }

    fun observeChapters(userId: String): Flow<List<Chapter>> =
        userDoc(userId).collection("chapters")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Chapter::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(compareBy<Chapter> { it.subject }.thenBy { it.orderIndex }.thenBy { it.name })
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, "users/$userId/chapters", userId)
                throw error
            }

    fun observeTopics(userId: String): Flow<List<Topic>> =
        userDoc(userId).collection("topics")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Topic::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(compareBy<Topic> { it.chapterId }.thenBy { it.orderIndex }.thenBy { it.name })
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, "users/$userId/topics", userId)
                throw error
            }

    fun observeRevisionSessions(userId: String): Flow<List<RevisionSession>> =
        userDoc(userId).collection("revision_sessions")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    RevisionSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.sessionDate }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, "users/$userId/revision_sessions", userId)
                throw error
            }

    fun observeCalendarEvents(userId: String): Flow<List<CalendarEvent>> =
        userDoc(userId).collection("calendar_events")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CalendarEvent::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(compareBy<CalendarEvent> { it.date }.thenBy { it.timeSlot })
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, "users/$userId/calendar_events", userId)
                throw error
            }

    fun observeTestRecords(userId: String): Flow<List<TestRecord>> =
        userDoc(userId).collection("test_records")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    TestRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.testDate }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, "users/$userId/test_records", userId)
                throw error
            }

    fun observeErrorRecords(userId: String): Flow<List<ErrorRecord>> =
        userDoc(userId).collection("error_records")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ErrorRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.dateRecorded }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, "users/$userId/error_records", userId)
                throw error
            }

    fun observeFormulaNotes(userId: String): Flow<List<FormulaNote>> =
        userDoc(userId).collection("formula_notes")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FormulaNote::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.title }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, "users/$userId/formula_notes", userId)
                throw error
            }

    private fun logFlowError(
        error: Throwable,
        op: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val toLog = firestoreError ?: error
        if (auth.currentUser?.uid == uid || firestoreError != null) {
            handleFirestoreError(toLog, op, path, auth)
        }
    }

    /**
     * Ensures UserProfile exists and seeds the editable Class 11 & 12 JEE syllabus catalogue
     * on first sign-in without adding any fake progress stats.
     */
    suspend fun ensureUserInitialized(displayName: String, email: String?) {
        val uid = requireUserId()
        val uRef = userDoc(uid)
        try {
            val snap = uRef.get().await()
            val existingProfile = if (snap.exists()) snap.toObject(UserProfile::class.java) else null
            if (existingProfile == null) {
                val profile = UserProfile(
                    id = uid,
                    userId = uid,
                    displayName = displayName.ifBlank { "JEE Aspirant" },
                    email = email,
                    seededInitialCatalog = true
                )
                uRef.set(profile.toCreateMap()).await()
                seedSyllabusCatalog(uid)
            } else if (!existingProfile.seededInitialCatalog) {
                seedSyllabusCatalog(uid)
                uRef.update(
                    mapOf(
                        "seededInitialCatalog" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, uRef.path, auth)
            throw e
        }
    }

    private suspend fun seedSyllabusCatalog(uid: String) {
        val chapters = JeeSyllabusCatalog.createSeedChapters(uid)
        val topics = JeeSyllabusCatalog.createSeedTopics(uid)
        val ops = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any>>>()
        chapters.forEach { ch ->
            ops.add(userDoc(uid).collection("chapters").document(ch.id) to ch.toCreateMap())
        }
        topics.forEach { tp ->
            ops.add(userDoc(uid).collection("topics").document(tp.id) to tp.toCreateMap())
        }
        ops.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { (ref, payload) -> batch.set(ref, payload) }
            batch.commit().await()
        }
    }

    suspend fun updateUserPreferences(updates: Map<String, Any>) {
        val uid = requireUserId()
        val uRef = userDoc(uid)
        try {
            uRef.update(updates + ("updatedAt" to FieldValue.serverTimestamp())).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, uRef.path, auth)
            throw e
        }
    }

    // ================= CHAPTERS =================

    suspend fun createChapter(chapter: Chapter): String {
        val uid = requireUserId()
        val id = chapter.id.takeIf { it.isNotBlank() } ?: "ch_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val ref = userDoc(uid).collection("chapters").document(id)
        try {
            ref.set(chapter.copy(userId = uid).toCreateMap()).await()
            return id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path, auth)
            throw e
        }
    }

    suspend fun updateChapterFields(chapterId: String, updates: Map<String, Any>) {
        val uid = requireUserId()
        require(chapterId.isNotBlank()) { "Chapter ID required" }
        val ref = userDoc(uid).collection("chapters").document(chapterId)
        try {
            ref.update(updates + ("updatedAt" to FieldValue.serverTimestamp())).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path, auth)
            throw e
        }
    }

    suspend fun deleteChapter(chapterId: String) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("chapters").document(chapterId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path, auth)
            throw e
        }
    }

    // ================= TOPICS =================

    suspend fun createTopic(topic: Topic): String {
        val uid = requireUserId()
        val id = topic.id.takeIf { it.isNotBlank() } ?: "tp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val ref = userDoc(uid).collection("topics").document(id)
        try {
            ref.set(topic.copy(userId = uid).toCreateMap()).await()
            return id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path, auth)
            throw e
        }
    }

    suspend fun updateTopicFields(topicId: String, updates: Map<String, Any>) {
        val uid = requireUserId()
        require(topicId.isNotBlank()) { "Topic ID required" }
        val ref = userDoc(uid).collection("topics").document(topicId)
        try {
            ref.update(updates + ("updatedAt" to FieldValue.serverTimestamp())).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path, auth)
            throw e
        }
    }

    // ================= REVISION SESSION COMMIT =================

    suspend fun recordRevisionSession(
        session: RevisionSession,
        chapter: Chapter,
        topic: Topic?,
        scheduleResult: ScheduleComputationResult,
        optionalError: ErrorRecord?
    ): String {
        val uid = requireUserId()
        val sessionId = "rs_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val sessionRef = userDoc(uid).collection("revision_sessions").document(sessionId)
        val chapterRef = userDoc(uid).collection("chapters").document(chapter.id)
        val today = session.sessionDate.ifBlank { IstDateUtils.todayIst() }

        try {
            val batch = db.batch()
            batch.set(
                sessionRef,
                session.copy(
                    userId = uid,
                    sessionDate = today,
                    newIntervalDays = scheduleResult.nextIntervalDays.toLong(),
                    nextRevisionDate = scheduleResult.nextRevisionDate,
                    rescheduleExplanation = scheduleResult.explanation
                ).toCreateMap()
            )

            val newChapterTotalQ = chapter.totalQuestions + session.questionsAttempted
            val newChapterCorrectQ = chapter.correctQuestions + session.questionsCorrect
            val newChapterIncorrectQ = chapter.incorrectQuestions + session.questionsIncorrect
            val newChapterUnsolvedQ = chapter.unsolvedQuestions + session.questionsUnsolved

            if (topic != null) {
                val topicRef = userDoc(uid).collection("topics").document(topic.id)
                batch.update(
                    topicRef,
                    mapOf(
                        "status" to scheduleResult.newStatus.name,
                        "initialLearningDate" to topic.initialLearningDate.ifBlank { today },
                        "completedRevisions" to (topic.completedRevisions + 1L),
                        "lastRevisionDate" to today,
                        "nextRevisionDate" to scheduleResult.nextRevisionDate,
                        "originalDueDate" to scheduleResult.originalTargetDate,
                        "currentIntervalDays" to scheduleResult.nextIntervalDays.toLong(),
                        "consecutiveSuccesses" to scheduleResult.newConsecutiveSuccesses.toLong(),
                        "understandingRating" to session.understandingRating.toDouble(),
                        "confidenceRating" to session.confidenceRating.toDouble(),
                        "difficultyRating" to session.difficultyRating.toDouble(),
                        "totalQuestions" to (topic.totalQuestions + session.questionsAttempted),
                        "correctQuestions" to (topic.correctQuestions + session.questionsCorrect),
                        "incorrectQuestions" to (topic.incorrectQuestions + session.questionsIncorrect),
                        "unsolvedQuestions" to (topic.unsolvedQuestions + session.questionsUnsolved),
                        "estimatedRetention" to scheduleResult.estimatedRetentionAfter,
                        "lastOutcome" to session.outcome,
                        "lastRescheduleReason" to scheduleResult.explanation.take(500),
                        "skipReason" to "",
                        "skippedUntilDate" to "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                // Also update aggregate question & last revision metadata on parent chapter
                val nextChapterStatus = if (chapter.statusEnum == ChapterStatus.NOT_STARTED) {
                    ChapterStatus.REVISION_IN_PROGRESS.name
                } else chapter.status
                batch.update(
                    chapterRef,
                    mapOf(
                        "status" to nextChapterStatus,
                        "initialLearningDate" to chapter.initialLearningDate.ifBlank { today },
                        "lastRevisionDate" to today,
                        "totalQuestions" to newChapterTotalQ,
                        "correctQuestions" to newChapterCorrectQ,
                        "incorrectQuestions" to newChapterIncorrectQ,
                        "unsolvedQuestions" to newChapterUnsolvedQ,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                val problemRating = if (session.questionsAttempted > 0) {
                    ((session.questionsCorrect.toDouble() / session.questionsAttempted.toDouble()) * 5.0).coerceIn(1.0, 5.0)
                } else session.understandingRating.toDouble()

                batch.update(
                    chapterRef,
                    mapOf(
                        "status" to scheduleResult.newStatus.name,
                        "initialLearningDate" to chapter.initialLearningDate.ifBlank { today },
                        "completedRevisions" to (chapter.completedRevisions + 1L),
                        "lastRevisionDate" to today,
                        "nextRevisionDate" to scheduleResult.nextRevisionDate,
                        "originalDueDate" to scheduleResult.originalTargetDate,
                        "currentIntervalDays" to scheduleResult.nextIntervalDays.toLong(),
                        "consecutiveSuccesses" to scheduleResult.newConsecutiveSuccesses.toLong(),
                        "understandingRating" to session.understandingRating.toDouble(),
                        "problemSolvingRating" to problemRating,
                        "confidenceRating" to session.confidenceRating.toDouble(),
                        "difficultyRating" to session.difficultyRating.toDouble(),
                        "totalQuestions" to newChapterTotalQ,
                        "correctQuestions" to newChapterCorrectQ,
                        "incorrectQuestions" to newChapterIncorrectQ,
                        "unsolvedQuestions" to newChapterUnsolvedQ,
                        "estimatedRetention" to scheduleResult.estimatedRetentionAfter,
                        "lastRescheduleReason" to scheduleResult.explanation.take(500),
                        "skipReason" to "",
                        "skippedUntilDate" to "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            if (optionalError != null) {
                val errId = "er_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                val errRef = userDoc(uid).collection("error_records").document(errId)
                batch.set(errRef, optionalError.copy(userId = uid).toCreateMap())
            }

            batch.commit().await()
            return sessionId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, sessionRef.path, auth)
            throw e
        }
    }

    // ================= CALENDAR EVENTS =================

    suspend fun createCalendarEvent(event: CalendarEvent): String {
        val uid = requireUserId()
        val id = event.id.takeIf { it.isNotBlank() } ?: "ev_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val ref = userDoc(uid).collection("calendar_events").document(id)
        try {
            ref.set(event.copy(userId = uid).toCreateMap()).await()
            return id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path, auth)
            throw e
        }
    }

    suspend fun updateCalendarEvent(eventId: String, updates: Map<String, Any>) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("calendar_events").document(eventId)
        try {
            ref.update(updates + ("updatedAt" to FieldValue.serverTimestamp())).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path, auth)
            throw e
        }
    }

    suspend fun deleteCalendarEvent(eventId: String) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("calendar_events").document(eventId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path, auth)
            throw e
        }
    }

    // ================= TEST RECORDS =================

    suspend fun createTestRecord(record: TestRecord): String {
        val uid = requireUserId()
        val id = record.id.takeIf { it.isNotBlank() } ?: "tr_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val ref = userDoc(uid).collection("test_records").document(id)
        try {
            ref.set(record.copy(userId = uid).toCreateMap()).await()
            return id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path, auth)
            throw e
        }
    }

    suspend fun deleteTestRecord(testId: String) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("test_records").document(testId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path, auth)
            throw e
        }
    }

    suspend fun recordChapterPracticeTest(
        chapter: Chapter,
        topic: Topic?,
        impact: ChapterTestScheduleImpact,
        durationMinutes: Int,
        testTitle: String
    ): String {
        val uid = requireUserId()
        val testId = "tr_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val testRef = userDoc(uid).collection("test_records").document(testId)
        val chapterRef = userDoc(uid).collection("chapters").document(chapter.id)
        val today = IstDateUtils.todayIst()

        val attempted = impact.testChapterResult.questionsAttempted.coerceAtLeast(1L)
        val correct = impact.testChapterResult.questionsCorrect.coerceIn(0L, attempted)
        val incorrect = (attempted - correct).coerceAtLeast(0L)

        val subject = chapter.subjectEnum
        val phyScore = if (subject == Subject.PHYSICS) correct.toDouble() else 0.0
        val phyMax = if (subject == Subject.PHYSICS) attempted.toDouble() else 0.0
        val chemScore = if (subject == Subject.CHEMISTRY) correct.toDouble() else 0.0
        val chemMax = if (subject == Subject.CHEMISTRY) attempted.toDouble() else 0.0
        val mathScore = if (subject == Subject.MATHEMATICS) correct.toDouble() else 0.0
        val mathMax = if (subject == Subject.MATHEMATICS) attempted.toDouble() else 0.0

        val record = TestRecord(
            userId = uid,
            testName = testTitle.trim().take(150).ifBlank { "AI Practice: ${topic?.name ?: chapter.name}" },
            testDate = today,
            testType = TestType.CHAPTER_TEST.name,
            physicsScore = phyScore,
            physicsMax = phyMax,
            chemistryScore = chemScore,
            chemistryMax = chemMax,
            mathsScore = mathScore,
            mathsMax = mathMax,
            totalMarks = correct.toDouble(),
            maxMarks = attempted.toDouble(),
            accuracyPercent = impact.accuracyPercent.coerceIn(0.0, 100.0),
            timeManagementNotes = "${durationMinutes.coerceAtLeast(1)} min • ${impact.scheduleResult.explanation}".take(1500),
            chapterResults = listOf(impact.testChapterResult.toMap())
        )

        val newChapterTotal = chapter.totalQuestions + attempted
        val newChapterCorrect = chapter.correctQuestions + correct
        val newChapterIncorrect = chapter.incorrectQuestions + incorrect
        val updatedProblemRating = if (newChapterTotal > 0L) {
            ((newChapterCorrect.toDouble() / newChapterTotal.toDouble()) * 5.0).coerceIn(1.0, 5.0)
        } else 3.0

        try {
            val batch = db.batch()
            batch.set(testRef, record.toCreateMap())

            if (topic != null) {
                val topicRef = userDoc(uid).collection("topics").document(topic.id)
                val topicUnderstanding = (impact.accuracyPercent / 20.0).coerceIn(1.0, 5.0)
                batch.update(
                    topicRef,
                    mapOf(
                        "status" to impact.scheduleResult.newStatus.name,
                        "initialLearningDate" to topic.initialLearningDate.ifBlank { today },
                        "nextRevisionDate" to impact.scheduleResult.nextRevisionDate,
                        "originalDueDate" to impact.scheduleResult.originalTargetDate,
                        "currentIntervalDays" to impact.scheduleResult.nextIntervalDays.toLong().coerceIn(1L, 3650L),
                        "consecutiveSuccesses" to impact.scheduleResult.newConsecutiveSuccesses.toLong().coerceAtLeast(0L),
                        "understandingRating" to topicUnderstanding,
                        "totalQuestions" to (topic.totalQuestions + attempted),
                        "correctQuestions" to (topic.correctQuestions + correct),
                        "incorrectQuestions" to (topic.incorrectQuestions + incorrect),
                        "estimatedRetention" to impact.scheduleResult.estimatedRetentionAfter.coerceIn(0.0, 100.0),
                        "lastRescheduleReason" to impact.scheduleResult.explanation.take(500),
                        "skipReason" to "",
                        "skippedUntilDate" to "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val nextChapterStatus = if (chapter.statusEnum == ChapterStatus.NOT_STARTED) {
                    ChapterStatus.REVISION_IN_PROGRESS.name
                } else chapter.status

                batch.update(
                    chapterRef,
                    mapOf(
                        "status" to nextChapterStatus,
                        "initialLearningDate" to chapter.initialLearningDate.ifBlank { today },
                        "totalQuestions" to newChapterTotal,
                        "correctQuestions" to newChapterCorrect,
                        "incorrectQuestions" to newChapterIncorrect,
                        "problemSolvingRating" to updatedProblemRating,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.update(
                    chapterRef,
                    mapOf(
                        "status" to impact.scheduleResult.newStatus.name,
                        "initialLearningDate" to chapter.initialLearningDate.ifBlank { today },
                        "nextRevisionDate" to impact.scheduleResult.nextRevisionDate,
                        "originalDueDate" to impact.scheduleResult.originalTargetDate,
                        "currentIntervalDays" to impact.scheduleResult.nextIntervalDays.toLong().coerceIn(1L, 3650L),
                        "consecutiveSuccesses" to impact.scheduleResult.newConsecutiveSuccesses.toLong().coerceAtLeast(0L),
                        "problemSolvingRating" to updatedProblemRating,
                        "totalQuestions" to newChapterTotal,
                        "correctQuestions" to newChapterCorrect,
                        "incorrectQuestions" to newChapterIncorrect,
                        "estimatedRetention" to impact.scheduleResult.estimatedRetentionAfter.coerceIn(0.0, 100.0),
                        "lastRescheduleReason" to impact.scheduleResult.explanation.take(500),
                        "skipReason" to "",
                        "skippedUntilDate" to "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            return testId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, testRef.path, auth)
            throw e
        }
    }

    // ================= ERROR NOTEBOOK =================

    suspend fun createErrorRecord(record: ErrorRecord): String {
        val uid = requireUserId()
        val id = record.id.takeIf { it.isNotBlank() } ?: "er_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val ref = userDoc(uid).collection("error_records").document(id)
        try {
            ref.set(record.copy(userId = uid).toCreateMap()).await()
            return id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path, auth)
            throw e
        }
    }

    suspend fun updateErrorRecord(errorId: String, updates: Map<String, Any>) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("error_records").document(errorId)
        try {
            ref.update(updates + ("updatedAt" to FieldValue.serverTimestamp())).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path, auth)
            throw e
        }
    }

    suspend fun deleteErrorRecord(errorId: String) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("error_records").document(errorId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path, auth)
            throw e
        }
    }

    // ================= FORMULA BANK =================

    suspend fun createFormulaNote(note: FormulaNote): String {
        val uid = requireUserId()
        val id = note.id.takeIf { it.isNotBlank() } ?: "fn_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val ref = userDoc(uid).collection("formula_notes").document(id)
        try {
            ref.set(note.copy(userId = uid).toCreateMap()).await()
            return id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path, auth)
            throw e
        }
    }

    suspend fun updateFormulaNote(noteId: String, updates: Map<String, Any>) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("formula_notes").document(noteId)
        try {
            ref.update(updates + ("updatedAt" to FieldValue.serverTimestamp())).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path, auth)
            throw e
        }
    }

    suspend fun deleteFormulaNote(noteId: String) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("formula_notes").document(noteId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path, auth)
            throw e
        }
    }

    // ================= BACKUP RESTORE =================

    suspend fun restoreFromBackup(bundle: BackupBundle) {
        val uid = requireUserId()
        val uRef = userDoc(uid)
        try {
            uRef.update(
                mapOf(
                    "syllabusVersion" to bundle.syllabusVersion,
                    "defaultIntervals" to bundle.defaultIntervals,
                    "dailyRevisionCapacity" to bundle.dailyRevisionCapacity,
                    "preferredStudyTime" to bundle.preferredStudyTime,
                    "blockedDates" to bundle.blockedDates.take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val ops = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any>>>()
            bundle.chapters.forEach { c ->
                val cid = c.id.ifBlank { "ch_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
                ops.add(uRef.collection("chapters").document(cid) to c.copy(userId = uid).toCreateMap())
            }
            bundle.topics.forEach { t ->
                val tid = t.id.ifBlank { "tp_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
                ops.add(uRef.collection("topics").document(tid) to t.copy(userId = uid).toCreateMap())
            }
            bundle.sessions.forEach { s ->
                val sid = s.id.ifBlank { "rs_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
                ops.add(uRef.collection("revision_sessions").document(sid) to s.copy(userId = uid).toCreateMap())
            }
            bundle.calendarEvents.forEach { e ->
                val eid = e.id.ifBlank { "ev_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
                ops.add(uRef.collection("calendar_events").document(eid) to e.copy(userId = uid).toCreateMap())
            }
            bundle.testRecords.forEach { tr ->
                val tid = tr.id.ifBlank { "tr_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
                ops.add(uRef.collection("test_records").document(tid) to tr.copy(userId = uid).toCreateMap())
            }
            bundle.errorRecords.forEach { er ->
                val eid = er.id.ifBlank { "er_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
                ops.add(uRef.collection("error_records").document(eid) to er.copy(userId = uid).toCreateMap())
            }
            bundle.formulaNotes.forEach { fn ->
                val fid = fn.id.ifBlank { "fn_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
                ops.add(uRef.collection("formula_notes").document(fid) to fn.copy(userId = uid).toCreateMap())
            }

            ops.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { (ref, payload) -> batch.set(ref, payload) }
                batch.commit().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, uRef.path, auth)
            throw e
        }
    }
}
