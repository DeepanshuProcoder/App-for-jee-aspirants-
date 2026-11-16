package com.example.domain.engine

import com.example.domain.model.CalendarEvent
import com.example.domain.model.Chapter
import com.example.domain.model.ErrorRecord
import com.example.domain.model.FormulaNote
import com.example.domain.model.RevisionSession
import com.example.domain.model.TestRecord
import com.example.domain.model.Topic
import com.example.domain.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject

data class BackupBundle(
    val exportedAt: String,
    val syllabusVersion: String,
    val defaultIntervals: List<Long>,
    val dailyRevisionCapacity: Long,
    val preferredStudyTime: String,
    val blockedDates: List<String>,
    val chapters: List<Chapter>,
    val topics: List<Topic>,
    val sessions: List<RevisionSession>,
    val calendarEvents: List<CalendarEvent>,
    val testRecords: List<TestRecord>,
    val errorRecords: List<ErrorRecord>,
    val formulaNotes: List<FormulaNote>
)

object BackupSerializer {

    fun toJsonString(
        profile: UserProfile?,
        chapters: List<Chapter>,
        topics: List<Topic>,
        sessions: List<RevisionSession>,
        events: List<CalendarEvent>,
        tests: List<TestRecord>,
        errors: List<ErrorRecord>,
        notes: List<FormulaNote>
    ): String {
        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("timezone", "Asia/Kolkata")
        root.put("exportedAt", IstDateUtils.formatDateTimeForExport())
        root.put("syllabusVersion", profile?.syllabusVersion ?: JeeSyllabusCatalog.DEFAULT_SYLLABUS_VERSION)
        root.put("defaultIntervals", JSONArray(profile?.defaultIntervals ?: listOf(1L, 3L, 7L, 14L, 30L, 60L, 90L)))
        root.put("dailyRevisionCapacity", profile?.dailyRevisionCapacity ?: 6L)
        root.put("preferredStudyTime", profile?.preferredStudyTime ?: "18:30")
        root.put("blockedDates", JSONArray(profile?.blockedDates ?: emptyList<String>()))

        root.put("chapters", JSONArray().apply {
            chapters.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("subject", c.subject)
                    put("section", c.section)
                    put("classLevel", c.classLevel)
                    put("orderIndex", c.orderIndex)
                    put("isArchived", c.isArchived)
                    put("syllabusVersion", c.syllabusVersion)
                    put("status", c.status)
                    put("initialLearningDate", c.initialLearningDate)
                    put("completedRevisions", c.completedRevisions)
                    put("lastRevisionDate", c.lastRevisionDate)
                    put("nextRevisionDate", c.nextRevisionDate)
                    put("originalDueDate", c.originalDueDate)
                    put("currentIntervalDays", c.currentIntervalDays)
                    put("consecutiveSuccesses", c.consecutiveSuccesses)
                    put("understandingRating", c.understandingRating)
                    put("problemSolvingRating", c.problemSolvingRating)
                    put("confidenceRating", c.confidenceRating)
                    put("difficultyRating", c.difficultyRating)
                    put("totalQuestions", c.totalQuestions)
                    put("correctQuestions", c.correctQuestions)
                    put("incorrectQuestions", c.incorrectQuestions)
                    put("unsolvedQuestions", c.unsolvedQuestions)
                    put("estimatedRetention", c.estimatedRetention)
                    put("importance", c.importance)
                    put("lastRescheduleReason", c.lastRescheduleReason)
                    put("personalNotes", c.personalNotes)
                    put("importantFormulas", c.importantFormulas)
                    put("commonMistakes", c.commonMistakes)
                    put("difficultQuestions", c.difficultQuestions)
                    put("skipReason", c.skipReason)
                    put("skippedUntilDate", c.skippedUntilDate)
                })
            }
        })

        root.put("topics", JSONArray().apply {
            topics.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("chapterId", t.chapterId)
                    put("subject", t.subject)
                    put("name", t.name)
                    put("orderIndex", t.orderIndex)
                    put("isArchived", t.isArchived)
                    put("status", t.status)
                    put("initialLearningDate", t.initialLearningDate)
                    put("completedRevisions", t.completedRevisions)
                    put("lastRevisionDate", t.lastRevisionDate)
                    put("nextRevisionDate", t.nextRevisionDate)
                    put("originalDueDate", t.originalDueDate)
                    put("currentIntervalDays", t.currentIntervalDays)
                    put("consecutiveSuccesses", t.consecutiveSuccesses)
                    put("understandingRating", t.understandingRating)
                    put("confidenceRating", t.confidenceRating)
                    put("difficultyRating", t.difficultyRating)
                    put("totalQuestions", t.totalQuestions)
                    put("correctQuestions", t.correctQuestions)
                    put("incorrectQuestions", t.incorrectQuestions)
                    put("unsolvedQuestions", t.unsolvedQuestions)
                    put("estimatedRetention", t.estimatedRetention)
                    put("lastOutcome", t.lastOutcome)
                    put("lastRescheduleReason", t.lastRescheduleReason)
                    put("notes", t.notes)
                    put("skipReason", t.skipReason)
                    put("skippedUntilDate", t.skippedUntilDate)
                })
            }
        })

        root.put("revision_sessions", JSONArray().apply {
            sessions.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id)
                    put("chapterId", s.chapterId)
                    put("chapterName", s.chapterName)
                    put("topicId", s.topicId)
                    put("topicName", s.topicName)
                    put("subject", s.subject)
                    put("sessionDate", s.sessionDate)
                    put("durationMinutes", s.durationMinutes)
                    put("activeRecallNotes", s.activeRecallNotes)
                    put("theoryChecklist", s.theoryChecklist)
                    put("questionsAttempted", s.questionsAttempted)
                    put("questionsCorrect", s.questionsCorrect)
                    put("questionsIncorrect", s.questionsIncorrect)
                    put("questionsWithHints", s.questionsWithHints)
                    put("questionsUnsolved", s.questionsUnsolved)
                    put("whatForgot", s.whatForgot)
                    put("unclearConcept", s.unclearConcept)
                    put("mistakeMade", s.mistakeMade)
                    put("needsPractice", s.needsPractice)
                    put("understandingRating", s.understandingRating)
                    put("confidenceRating", s.confidenceRating)
                    put("difficultyRating", s.difficultyRating)
                    put("outcome", s.outcome)
                    put("previousIntervalDays", s.previousIntervalDays)
                    put("newIntervalDays", s.newIntervalDays)
                    put("nextRevisionDate", s.nextRevisionDate)
                    put("rescheduleExplanation", s.rescheduleExplanation)
                })
            }
        })

        root.put("calendar_events", JSONArray().apply {
            events.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id)
                    put("title", e.title)
                    put("eventType", e.eventType)
                    put("subject", e.subject)
                    put("date", e.date)
                    put("timeSlot", e.timeSlot)
                    put("chapterId", e.chapterId)
                    put("notes", e.notes)
                    put("isCompleted", e.isCompleted)
                })
            }
        })

        root.put("test_records", JSONArray().apply {
            tests.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("testName", t.testName)
                    put("testDate", t.testDate)
                    put("testType", t.testType)
                    put("physicsScore", t.physicsScore)
                    put("physicsMax", t.physicsMax)
                    put("chemistryScore", t.chemistryScore)
                    put("chemistryMax", t.chemistryMax)
                    put("mathsScore", t.mathsScore)
                    put("mathsMax", t.mathsMax)
                    put("totalMarks", t.totalMarks)
                    put("maxMarks", t.maxMarks)
                    put("accuracyPercent", t.accuracyPercent)
                    put("timeManagementNotes", t.timeManagementNotes)
                })
            }
        })

        root.put("error_records", JSONArray().apply {
            errors.forEach { err ->
                put(JSONObject().apply {
                    put("id", err.id)
                    put("subject", err.subject)
                    put("chapterId", err.chapterId)
                    put("chapterName", err.chapterName)
                    put("topicId", err.topicId)
                    put("topicName", err.topicName)
                    put("questionRef", err.questionRef)
                    put("errorCategory", err.errorCategory)
                    put("mistakeExplanation", err.mistakeExplanation)
                    put("correctApproach", err.correctApproach)
                    put("importantLesson", err.importantLesson)
                    put("dateRecorded", err.dateRecorded)
                    put("reattemptStatus", err.reattemptStatus)
                    put("scheduledRevisionDate", err.scheduledRevisionDate)
                })
            }
        })

        root.put("formula_notes", JSONArray().apply {
            notes.forEach { n ->
                put(JSONObject().apply {
                    put("id", n.id)
                    put("subject", n.subject)
                    put("chapterId", n.chapterId)
                    put("chapterName", n.chapterName)
                    put("noteType", n.noteType)
                    put("title", n.title)
                    put("content", n.content)
                    put("latexFormula", n.latexFormula)
                    put("attachmentUri", n.attachmentUri)
                })
            }
        })

        return root.toString(2)
    }

    fun fromJsonString(json: String, userId: String): BackupBundle {
        val root = JSONObject(json)
        val intervalsJson = root.optJSONArray("defaultIntervals")
        val intervals = mutableListOf<Long>()
        if (intervalsJson != null) {
            for (i in 0 until intervalsJson.length()) {
                intervals.add(intervalsJson.optLong(i, 1L).coerceAtLeast(1L))
            }
        }
        val blockedJson = root.optJSONArray("blockedDates")
        val blocked = mutableListOf<String>()
        if (blockedJson != null) {
            for (i in 0 until blockedJson.length()) {
                blockedJson.optString(i)?.takeIf { it.isNotBlank() }?.let { blocked.add(it) }
            }
        }

        val chapters = mutableListOf<Chapter>()
        val chArr = root.optJSONArray("chapters") ?: JSONArray()
        for (i in 0 until chArr.length()) {
            val o = chArr.getJSONObject(i)
            chapters.add(
                Chapter(
                    id = o.optString("id"),
                    userId = userId,
                    name = o.optString("name"),
                    subject = o.optString("subject", "PHYSICS"),
                    section = o.optString("section"),
                    classLevel = o.optString("classLevel", "CLASS_11"),
                    orderIndex = o.optLong("orderIndex"),
                    isArchived = o.optBoolean("isArchived"),
                    syllabusVersion = o.optString("syllabusVersion", JeeSyllabusCatalog.DEFAULT_SYLLABUS_VERSION),
                    status = o.optString("status", "NOT_STARTED"),
                    initialLearningDate = o.optString("initialLearningDate"),
                    completedRevisions = o.optLong("completedRevisions"),
                    lastRevisionDate = o.optString("lastRevisionDate"),
                    nextRevisionDate = o.optString("nextRevisionDate"),
                    originalDueDate = o.optString("originalDueDate"),
                    currentIntervalDays = o.optLong("currentIntervalDays"),
                    consecutiveSuccesses = o.optLong("consecutiveSuccesses"),
                    understandingRating = o.optDouble("understandingRating", 0.0),
                    problemSolvingRating = o.optDouble("problemSolvingRating", 0.0),
                    confidenceRating = o.optDouble("confidenceRating", 0.0),
                    difficultyRating = o.optDouble("difficultyRating", 0.0),
                    totalQuestions = o.optLong("totalQuestions"),
                    correctQuestions = o.optLong("correctQuestions"),
                    incorrectQuestions = o.optLong("incorrectQuestions"),
                    unsolvedQuestions = o.optLong("unsolvedQuestions"),
                    estimatedRetention = o.optDouble("estimatedRetention", 0.0),
                    importance = o.optLong("importance", 3L),
                    lastRescheduleReason = o.optString("lastRescheduleReason"),
                    personalNotes = o.optString("personalNotes"),
                    importantFormulas = o.optString("importantFormulas"),
                    commonMistakes = o.optString("commonMistakes"),
                    difficultQuestions = o.optString("difficultQuestions"),
                    skipReason = o.optString("skipReason"),
                    skippedUntilDate = o.optString("skippedUntilDate")
                )
            )
        }

        val topics = mutableListOf<Topic>()
        val topArr = root.optJSONArray("topics") ?: JSONArray()
        for (i in 0 until topArr.length()) {
            val o = topArr.getJSONObject(i)
            topics.add(
                Topic(
                    id = o.optString("id"),
                    userId = userId,
                    chapterId = o.optString("chapterId"),
                    subject = o.optString("subject", "PHYSICS"),
                    name = o.optString("name"),
                    orderIndex = o.optLong("orderIndex"),
                    isArchived = o.optBoolean("isArchived"),
                    status = o.optString("status", "NOT_STARTED"),
                    initialLearningDate = o.optString("initialLearningDate"),
                    completedRevisions = o.optLong("completedRevisions"),
                    lastRevisionDate = o.optString("lastRevisionDate"),
                    nextRevisionDate = o.optString("nextRevisionDate"),
                    originalDueDate = o.optString("originalDueDate"),
                    currentIntervalDays = o.optLong("currentIntervalDays"),
                    consecutiveSuccesses = o.optLong("consecutiveSuccesses"),
                    understandingRating = o.optDouble("understandingRating", 0.0),
                    confidenceRating = o.optDouble("confidenceRating", 0.0),
                    difficultyRating = o.optDouble("difficultyRating", 0.0),
                    totalQuestions = o.optLong("totalQuestions"),
                    correctQuestions = o.optLong("correctQuestions"),
                    incorrectQuestions = o.optLong("incorrectQuestions"),
                    unsolvedQuestions = o.optLong("unsolvedQuestions"),
                    estimatedRetention = o.optDouble("estimatedRetention", 0.0),
                    lastOutcome = o.optString("lastOutcome"),
                    lastRescheduleReason = o.optString("lastRescheduleReason"),
                    notes = o.optString("notes"),
                    skipReason = o.optString("skipReason"),
                    skippedUntilDate = o.optString("skippedUntilDate")
                )
            )
        }

        val sessions = mutableListOf<RevisionSession>()
        val sessArr = root.optJSONArray("revision_sessions") ?: JSONArray()
        for (i in 0 until sessArr.length()) {
            val o = sessArr.getJSONObject(i)
            sessions.add(
                RevisionSession(
                    id = o.optString("id"),
                    userId = userId,
                    chapterId = o.optString("chapterId"),
                    chapterName = o.optString("chapterName"),
                    topicId = o.optString("topicId"),
                    topicName = o.optString("topicName"),
                    subject = o.optString("subject", "PHYSICS"),
                    sessionDate = o.optString("sessionDate"),
                    durationMinutes = o.optLong("durationMinutes"),
                    activeRecallNotes = o.optString("activeRecallNotes"),
                    theoryChecklist = o.optString("theoryChecklist"),
                    questionsAttempted = o.optLong("questionsAttempted"),
                    questionsCorrect = o.optLong("questionsCorrect"),
                    questionsIncorrect = o.optLong("questionsIncorrect"),
                    questionsWithHints = o.optLong("questionsWithHints"),
                    questionsUnsolved = o.optLong("questionsUnsolved"),
                    whatForgot = o.optString("whatForgot"),
                    unclearConcept = o.optString("unclearConcept"),
                    mistakeMade = o.optString("mistakeMade"),
                    needsPractice = o.optString("needsPractice"),
                    understandingRating = o.optLong("understandingRating", 3L),
                    confidenceRating = o.optLong("confidenceRating", 3L),
                    difficultyRating = o.optLong("difficultyRating", 3L),
                    outcome = o.optString("outcome", "GOOD"),
                    previousIntervalDays = o.optLong("previousIntervalDays"),
                    newIntervalDays = o.optLong("newIntervalDays", 1L),
                    nextRevisionDate = o.optString("nextRevisionDate"),
                    rescheduleExplanation = o.optString("rescheduleExplanation")
                )
            )
        }

        val events = mutableListOf<CalendarEvent>()
        val evArr = root.optJSONArray("calendar_events") ?: JSONArray()
        for (i in 0 until evArr.length()) {
            val o = evArr.getJSONObject(i)
            events.add(
                CalendarEvent(
                    id = o.optString("id"),
                    userId = userId,
                    title = o.optString("title"),
                    eventType = o.optString("eventType", "STUDY_TARGET"),
                    subject = o.optString("subject", "ALL"),
                    date = o.optString("date"),
                    timeSlot = o.optString("timeSlot", "18:00"),
                    chapterId = o.optString("chapterId"),
                    notes = o.optString("notes"),
                    isCompleted = o.optBoolean("isCompleted")
                )
            )
        }

        val tests = mutableListOf<TestRecord>()
        val tArr = root.optJSONArray("test_records") ?: JSONArray()
        for (i in 0 until tArr.length()) {
            val o = tArr.getJSONObject(i)
            tests.add(
                TestRecord(
                    id = o.optString("id"),
                    userId = userId,
                    testName = o.optString("testName"),
                    testDate = o.optString("testDate"),
                    testType = o.optString("testType", "ALLEN_MINOR"),
                    physicsScore = o.optDouble("physicsScore", 0.0),
                    physicsMax = o.optDouble("physicsMax", 100.0),
                    chemistryScore = o.optDouble("chemistryScore", 0.0),
                    chemistryMax = o.optDouble("chemistryMax", 100.0),
                    mathsScore = o.optDouble("mathsScore", 0.0),
                    mathsMax = o.optDouble("mathsMax", 100.0),
                    totalMarks = o.optDouble("totalMarks", 0.0),
                    maxMarks = o.optDouble("maxMarks", 300.0),
                    accuracyPercent = o.optDouble("accuracyPercent", 0.0),
                    timeManagementNotes = o.optString("timeManagementNotes")
                )
            )
        }

        val errors = mutableListOf<ErrorRecord>()
        val eArr = root.optJSONArray("error_records") ?: JSONArray()
        for (i in 0 until eArr.length()) {
            val o = eArr.getJSONObject(i)
            errors.add(
                ErrorRecord(
                    id = o.optString("id"),
                    userId = userId,
                    subject = o.optString("subject", "PHYSICS"),
                    chapterId = o.optString("chapterId"),
                    chapterName = o.optString("chapterName"),
                    topicId = o.optString("topicId"),
                    topicName = o.optString("topicName"),
                    questionRef = o.optString("questionRef"),
                    errorCategory = o.optString("errorCategory", "CONCEPTUAL"),
                    mistakeExplanation = o.optString("mistakeExplanation"),
                    correctApproach = o.optString("correctApproach"),
                    importantLesson = o.optString("importantLesson"),
                    dateRecorded = o.optString("dateRecorded"),
                    reattemptStatus = o.optString("reattemptStatus", "PENDING"),
                    scheduledRevisionDate = o.optString("scheduledRevisionDate")
                )
            )
        }

        val notes = mutableListOf<FormulaNote>()
        val nArr = root.optJSONArray("formula_notes") ?: JSONArray()
        for (i in 0 until nArr.length()) {
            val o = nArr.getJSONObject(i)
            notes.add(
                FormulaNote(
                    id = o.optString("id"),
                    userId = userId,
                    subject = o.optString("subject", "PHYSICS"),
                    chapterId = o.optString("chapterId"),
                    chapterName = o.optString("chapterName"),
                    noteType = o.optString("noteType", "FORMULA"),
                    title = o.optString("title"),
                    content = o.optString("content"),
                    latexFormula = o.optString("latexFormula"),
                    attachmentUri = o.optString("attachmentUri")
                )
            )
        }

        return BackupBundle(
            exportedAt = root.optString("exportedAt"),
            syllabusVersion = root.optString("syllabusVersion", JeeSyllabusCatalog.DEFAULT_SYLLABUS_VERSION),
            defaultIntervals = intervals.ifEmpty { listOf(1L, 3L, 7L, 14L, 30L, 60L, 90L) },
            dailyRevisionCapacity = root.optLong("dailyRevisionCapacity", 6L),
            preferredStudyTime = root.optString("preferredStudyTime", "18:30"),
            blockedDates = blocked,
            chapters = chapters,
            topics = topics,
            sessions = sessions,
            calendarEvents = events,
            testRecords = tests,
            errorRecords = errors,
            formulaNotes = notes
        )
    }

    fun toCsvSummary(
        chapters: List<Chapter>,
        sessions: List<RevisionSession>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("TYPE,SUBJECT,CHAPTER,TOPIC,CLASS,STATUS,COMPLETED_REVISIONS,INTERVAL_DAYS,NEXT_REVISION_DATE,EST_RETENTION,ACCURACY_PERCENT")
        chapters.forEach { c ->
            sb.appendLine(
                listOf(
                    "CHAPTER",
                    c.subject,
                    csvEscape(c.name),
                    "",
                    c.classLevel,
                    c.status,
                    c.completedRevisions.toString(),
                    c.currentIntervalDays.toString(),
                    c.nextRevisionDate,
                    "%.1f".format(c.estimatedRetention),
                    "%.1f".format(c.accuracyPercent)
                ).joinToString(",")
            )
        }
        sb.appendLine()
        sb.appendLine("SESSION_DATE,SUBJECT,CHAPTER,TOPIC,OUTCOME,DURATION_MIN,ATTEMPTED,CORRECT,PREV_INTERVAL,NEW_INTERVAL,NEXT_DATE,EXPLANATION")
        sessions.forEach { s ->
            sb.appendLine(
                listOf(
                    s.sessionDate,
                    s.subject,
                    csvEscape(s.chapterName),
                    csvEscape(s.topicName),
                    s.outcome,
                    s.durationMinutes.toString(),
                    s.questionsAttempted.toString(),
                    s.questionsCorrect.toString(),
                    s.previousIntervalDays.toString(),
                    s.newIntervalDays.toString(),
                    s.nextRevisionDate,
                    csvEscape(s.rescheduleExplanation)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    private fun csvEscape(raw: String): String {
        val cleaned = raw.replace("\"", "\"\"").replace("\n", " ")
        return "\"$cleaned\""
    }
}
