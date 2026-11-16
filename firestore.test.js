const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_jee_11";
const BOB_UID = "bob_jee_12";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const serverTs = () => new Date();

function validUserProfile(uid) {
  const now = serverTs();
  return {
    userId: uid,
    displayName: "Aarav Sharma",
    email: "aarav@example.com",
    classLevel: "CLASS_11",
    syllabusVersion: "JEE-2026-v1",
    defaultIntervals: [1, 3, 7, 14, 30, 60, 90],
    dailyRevisionCapacity: 6,
    preferredStudyTime: "18:30",
    darkModePreference: "DARK",
    notificationsEnabled: false,
    notifyTodayQueue: true,
    notifyOverdue: true,
    notifyTests: true,
    notifyWeakTopics: true,
    notifyWeeklyReport: true,
    blockedDates: [],
    seededInitialCatalog: true,
    createdAt: now,
    updatedAt: now,
  };
}

function validChapter(uid) {
  const now = serverTs();
  return {
    userId: uid,
    name: "Laws of Motion",
    subject: "PHYSICS",
    section: "",
    classLevel: "CLASS_11",
    orderIndex: 5,
    isArchived: false,
    syllabusVersion: "JEE-2026-v1",
    status: "NOT_STARTED",
    initialLearningDate: "",
    completedRevisions: 0,
    lastRevisionDate: "",
    nextRevisionDate: "",
    originalDueDate: "",
    currentIntervalDays: 0,
    consecutiveSuccesses: 0,
    understandingRating: 0.0,
    problemSolvingRating: 0.0,
    confidenceRating: 0.0,
    difficultyRating: 0.0,
    totalQuestions: 0,
    correctQuestions: 0,
    incorrectQuestions: 0,
    unsolvedQuestions: 0,
    estimatedRetention: 0.0,
    importance: 4,
    lastRescheduleReason: "",
    personalNotes: "",
    importantFormulas: "",
    commonMistakes: "",
    difficultQuestions: "",
    skipReason: "",
    skippedUntilDate: "",
    createdAt: now,
    updatedAt: now,
  };
}

function validTopic(uid, chapterId) {
  const now = serverTs();
  return {
    userId: uid,
    chapterId,
    subject: "PHYSICS",
    name: "Friction",
    orderIndex: 4,
    isArchived: false,
    status: "NOT_STARTED",
    initialLearningDate: "",
    completedRevisions: 0,
    lastRevisionDate: "",
    nextRevisionDate: "",
    originalDueDate: "",
    currentIntervalDays: 0,
    consecutiveSuccesses: 0,
    understandingRating: 0.0,
    confidenceRating: 0.0,
    difficultyRating: 0.0,
    totalQuestions: 0,
    correctQuestions: 0,
    incorrectQuestions: 0,
    unsolvedQuestions: 0,
    estimatedRetention: 0.0,
    lastOutcome: "",
    lastRescheduleReason: "",
    notes: "",
    skipReason: "",
    skippedUntilDate: "",
    createdAt: now,
    updatedAt: now,
  };
}

function validSession(uid, chapterId, topicId) {
  const now = serverTs();
  return {
    userId: uid,
    chapterId,
    chapterName: "Laws of Motion",
    topicId,
    topicName: "Friction",
    subject: "PHYSICS",
    sessionDate: "2026-11-16",
    durationMinutes: 25,
    activeRecallNotes: "Static vs kinetic friction inequalities",
    theoryChecklist: "Definitions,Formulas",
    questionsAttempted: 12,
    questionsCorrect: 10,
    questionsIncorrect: 1,
    questionsWithHints: 1,
    questionsUnsolved: 1,
    whatForgot: "Angle of repose edge case",
    unclearConcept: "",
    mistakeMade: "Wrong normal force on incline",
    needsPractice: "Two-block friction problems",
    understandingRating: 4,
    confidenceRating: 4,
    difficultyRating: 3,
    outcome: "GOOD",
    previousIntervalDays: 3,
    newIntervalDays: 7,
    nextRevisionDate: "2026-11-23",
    rescheduleExplanation: "Followed normal sequence 3d -> 7d based on 83% accuracy.",
    createdAt: now,
    updatedAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any user collection", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("users").doc(ALICE_UID).get());
  await assertFails(db.collection("users").doc(ALICE_UID).collection("chapters").get());
  await assertFails(
    db.collection("users").doc(ALICE_UID).collection("chapters").doc("ch1").set(validChapter(ALICE_UID))
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's profile, chapters, or sessions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chapters").doc("phys_lom").set(validChapter(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("chapters").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("chapters").doc("phys_lom").delete()
  );
});

test("Authenticated owner can CRUD profile, chapter, topic, and create immutable session", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID))
  );
  const chRef = aliceDb.collection("users").doc(ALICE_UID).collection("chapters").doc("phys_lom");
  await assertSucceeds(chRef.set(validChapter(ALICE_UID)));
  await assertSucceeds(chRef.update({ status: "REVISION_IN_PROGRESS", completedRevisions: 1, updatedAt: serverTs() }));

  const topicRef = aliceDb.collection("users").doc(ALICE_UID).collection("topics").doc("topic_friction");
  await assertSucceeds(topicRef.set(validTopic(ALICE_UID, "phys_lom")));

  const sessionRef = aliceDb.collection("users").doc(ALICE_UID).collection("revision_sessions").doc("sess_1");
  await assertSucceeds(sessionRef.set(validSession(ALICE_UID, "phys_lom", "topic_friction")));
  // Revision session updates are forbidden (immutable history)
  await assertFails(sessionRef.update({ outcome: "EASY" }));
});

test("Shadow update with ghost field or invalid enum is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const chRef = aliceDb.collection("users").doc(ALICE_UID).collection("chapters").doc("phys_lom");
  const badChapter = { ...validChapter(ALICE_UID), ghostAdmin: true };
  await assertFails(chRef.set(badChapter));

  const badEnumChapter = { ...validChapter(ALICE_UID), subject: "BIOLOGY" };
  await assertFails(chRef.set(badEnumChapter));
});

test("Owner can create calendar_events, test_records, error_records, and formula_notes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = serverTs();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("calendar_events").doc("ev1").set({
      userId: ALICE_UID,
      title: "ALLEN Minor Test 4",
      eventType: "ALLEN_TEST",
      subject: "ALL",
      date: "2026-11-22",
      timeSlot: "09:00",
      chapterId: "",
      notes: "Full syllabus up to Rotation",
      isCompleted: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("test_records").doc("t1").set({
      userId: ALICE_UID,
      testName: "ALLEN Minor Test 3",
      testDate: "2026-11-10",
      testType: "ALLEN_MINOR",
      physicsScore: 76.0,
      physicsMax: 100.0,
      chemistryScore: 82.0,
      chemistryMax: 100.0,
      mathsScore: 68.0,
      mathsMax: 100.0,
      totalMarks: 226.0,
      maxMarks: 300.0,
      accuracyPercent: 84.5,
      timeManagementNotes: "Spent extra time on Rotation numericals",
      chapterResults: [{ chapterId: "phys_lom", chapterName: "Laws of Motion", subject: "PHYSICS", accuracyPercent: 75.0, weakFlag: false }],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("error_records").doc("e1").set({
      userId: ALICE_UID,
      subject: "PHYSICS",
      chapterId: "phys_lom",
      chapterName: "Laws of Motion",
      topicId: "topic_friction",
      topicName: "Friction",
      questionRef: "HCV Q24",
      errorCategory: "CONCEPTUAL",
      mistakeExplanation: "Assumed static friction always equals mu*N",
      correctApproach: "Check f_req <= f_max first",
      importantLesson: "Static friction is self-adjusting",
      dateRecorded: "2026-11-16",
      reattemptStatus: "PENDING",
      scheduledRevisionDate: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("formula_notes").doc("f1").set({
      userId: ALICE_UID,
      subject: "PHYSICS",
      chapterId: "phys_lom",
      chapterName: "Laws of Motion",
      noteType: "FORMULA",
      title: "Limiting Friction",
      content: "Maximum static friction proportional to normal reaction.",
      latexFormula: "f_{max} = \\mu_s N",
      attachmentUri: "",
      createdAt: now,
      updatedAt: now,
    })
  );
});
