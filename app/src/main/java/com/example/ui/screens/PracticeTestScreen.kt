package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.engine.IstDateUtils
import com.example.domain.engine.LatexFormatter
import com.example.domain.model.Chapter
import com.example.domain.model.ChapterStatus
import com.example.domain.model.EvaluatedQuestionResult
import com.example.domain.model.PracticeDifficulty
import com.example.domain.model.PracticeDifficultyFilter
import com.example.domain.model.PracticeQuestionType
import com.example.domain.model.PracticeTestOutcomeSummary
import com.example.domain.model.PracticeTestUiState
import com.example.domain.model.Subject
import com.example.domain.model.Topic
import com.example.ui.components.StatusChip
import com.example.ui.components.SubjectBadge
import com.example.ui.theme.AlertRose
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.SolarAmber
import com.example.ui.viewmodel.JeeAppUiState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeTestScreen(
    uiState: JeeAppUiState,
    practiceState: PracticeTestUiState,
    initialChapterId: String?,
    initialTopicId: String?,
    onGenerateTest: (
        chapter: Chapter,
        topic: Topic?,
        questionCount: Int,
        difficultyFilter: PracticeDifficultyFilter,
        allowedTypes: Set<PracticeQuestionType>
    ) -> Unit,
    onUpdateAnswer: (questionId: String, answer: String) -> Unit,
    onGoToQuestion: (index: Int) -> Unit,
    onSubmitTest: (elapsedSeconds: Int) -> Unit,
    onSaveErrorToNotebook: (chapter: Chapter, topic: Topic?, evaluated: EvaluatedQuestionResult) -> Unit,
    onResetPracticeTest: () -> Unit,
    onStartRevision: (chapterId: String, topicId: String?) -> Unit,
    onBack: () -> Unit
) {
    var showExitConfirm by remember { mutableStateOf(false) }

    val handleBack = {
        when (practiceState) {
            is PracticeTestUiState.InProgress -> showExitConfirm = true
            is PracticeTestUiState.Generating -> {
                onResetPracticeTest()
                onBack()
            }
            else -> onBack()
        }
    }

    BackHandler { handleBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val subtitle = when (practiceState) {
                        is PracticeTestUiState.InProgress ->
                            practiceState.topic?.name ?: practiceState.chapter.name
                        is PracticeTestUiState.Completed ->
                            practiceState.summary.topicName ?: practiceState.summary.chapterName
                        is PracticeTestUiState.Generating ->
                            practiceState.topicName ?: practiceState.chapterName
                        else -> "AI Chapter & Topic Practice"
                    }
                    Column {
                        Text(
                            text = if (practiceState is PracticeTestUiState.InProgress) "Practice Test" else "Chapter & Topic Test",
                            maxLines = 1
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = handleBack,
                        modifier = Modifier.testTag("practice_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when (practiceState) {
            is PracticeTestUiState.Idle,
            is PracticeTestUiState.Error -> {
                PracticeTestSetupContent(
                    uiState = uiState,
                    errorMessage = (practiceState as? PracticeTestUiState.Error)?.message,
                    initialChapterId = initialChapterId,
                    initialTopicId = initialTopicId,
                    onGenerateTest = onGenerateTest,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is PracticeTestUiState.Generating -> {
                PracticeTestGeneratingContent(
                    chapterName = practiceState.chapterName,
                    topicName = practiceState.topicName,
                    requestedCount = practiceState.requestedCount,
                    onCancel = onResetPracticeTest,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is PracticeTestUiState.InProgress -> {
                PracticeTestRunnerContent(
                    state = practiceState,
                    onUpdateAnswer = onUpdateAnswer,
                    onGoToQuestion = onGoToQuestion,
                    onSubmitTest = onSubmitTest,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is PracticeTestUiState.Completed -> {
                val chapterObj = remember(uiState.chapters, practiceState.summary.chapterId) {
                    uiState.chapters.firstOrNull { it.id == practiceState.summary.chapterId }
                }
                val topicObj = remember(uiState.topics, practiceState.summary.topicId) {
                    practiceState.summary.topicId?.let { tid ->
                        uiState.topics.firstOrNull { it.id == tid }
                    }
                }
                PracticeTestResultsContent(
                    summary = practiceState.summary,
                    onSaveErrorToNotebook = { evaluated ->
                        if (chapterObj != null) {
                            onSaveErrorToNotebook(chapterObj, topicObj, evaluated)
                        }
                    },
                    onStartRevision = {
                        onResetPracticeTest()
                        onStartRevision(practiceState.summary.chapterId, practiceState.summary.topicId)
                    },
                    onNewTest = onResetPracticeTest,
                    onDone = {
                        onResetPracticeTest()
                        onBack()
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("Exit Practice Test?") },
            text = { Text("Your current answers have not been submitted yet. Leaving now will discard this attempt.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirm = false
                        onResetPracticeTest()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRose)
                ) {
                    Text("Discard & Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text("Keep Solving")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PracticeTestSetupContent(
    uiState: JeeAppUiState,
    errorMessage: String?,
    initialChapterId: String?,
    initialTopicId: String?,
    onGenerateTest: (Chapter, Topic?, Int, PracticeDifficultyFilter, Set<PracticeQuestionType>) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeChapters = remember(uiState.chapters) {
        uiState.chapters.filter { !it.isArchived }
    }
    val preselectedChapter = remember(activeChapters, initialChapterId) {
        activeChapters.firstOrNull { it.id == initialChapterId } ?: activeChapters.firstOrNull()
    }

    var selectedSubject by rememberSaveable(preselectedChapter?.id) {
        mutableStateOf(preselectedChapter?.subjectEnum ?: Subject.PHYSICS)
    }
    var selectedChapterId by rememberSaveable(preselectedChapter?.id) {
        mutableStateOf(preselectedChapter?.id ?: "")
    }
    var selectedTopicId by rememberSaveable(initialTopicId) {
        mutableStateOf(initialTopicId)
    }
    var questionCount by rememberSaveable { mutableIntStateOf(5) }
    var difficultyFilter by rememberSaveable { mutableStateOf(PracticeDifficultyFilter.MIXED) }
    var selectedTypes by remember {
        mutableStateOf(setOf(PracticeQuestionType.MCQ, PracticeQuestionType.NUMERICAL, PracticeQuestionType.SHORT_ANSWER))
    }
    var showChapterPicker by remember { mutableStateOf(false) }

    val subjectChapters = remember(activeChapters, selectedSubject) {
        activeChapters.filter { it.subjectEnum == selectedSubject }
    }
    val selectedChapter = remember(activeChapters, subjectChapters, selectedChapterId) {
        subjectChapters.firstOrNull { it.id == selectedChapterId } ?: subjectChapters.firstOrNull()
    }
    val chapterTopics = remember(uiState.topics, selectedChapter?.id) {
        if (selectedChapter == null) emptyList()
        else uiState.topics.filter { it.chapterId == selectedChapter.id && !it.isArchived }
            .sortedBy { it.orderIndex }
    }
    val selectedTopic = remember(chapterTopics, selectedTopicId) {
        selectedTopicId?.let { tid -> chapterTopics.firstOrNull { it.id == tid } }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!errorMessage.isNullOrBlank()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AlertRose.copy(alpha = 0.14f)),
                    border = BorderStroke(1.dp, AlertRose.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Rounded.ErrorOutline,
                            contentDescription = "Generation error",
                            tint = AlertRose
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Could not generate test",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AlertRose
                            )
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Intro & Purpose
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = "AI practice generator",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Targeted Practice & Auto-Rescheduling",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Generates custom JEE MCQs, numerical & short-answer questions. Weak scores automatically schedule earlier revisions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Subject & Chapter Selection
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("1. Subject & Chapter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Subject.entries.forEach { subj ->
                            FilterChip(
                                selected = selectedSubject == subj,
                                onClick = {
                                    selectedSubject = subj
                                    val firstInSubj = activeChapters.firstOrNull { it.subjectEnum == subj }
                                    selectedChapterId = firstInSubj?.id ?: ""
                                    selectedTopicId = null
                                },
                                label = { Text(subj.shortLabel) },
                                modifier = Modifier.testTag("practice_subject_${subj.name.lowercase()}")
                            )
                        }
                    }

                    if (selectedChapter != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showChapterPicker = true }
                                .testTag("select_chapter_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        SubjectBadge(selectedChapter.subjectEnum)
                                        StatusChip(selectedChapter.statusEnum)
                                    }
                                    Text(
                                        text = selectedChapter.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val statsLine = if (selectedChapter.totalQuestions > 0) {
                                        "${selectedChapter.accuracyPercent.roundToInt()}% accuracy • ${selectedChapter.totalQuestions} Qs solved"
                                    } else {
                                        "${selectedChapter.classEnum.label} • ${chapterTopics.size} topic(s)"
                                    }
                                    Text(
                                        text = statsLine,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { showChapterPicker = true }) {
                                    Text("Change")
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No active chapters found in ${selectedSubject.label}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Topic Selection
        if (selectedChapter != null) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("2. Topic Focus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (chapterTopics.isEmpty()) {
                                "Full chapter coverage (no custom subtopics added yet)."
                            } else {
                                "Test a specific topic to update its individual spaced-repetition cycle, or choose Full Chapter."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedTopic == null,
                                onClick = { selectedTopicId = null },
                                label = { Text("Full Chapter") },
                                modifier = Modifier.testTag("topic_chip_full_chapter")
                            )
                            chapterTopics.forEach { tp ->
                                FilterChip(
                                    selected = selectedTopic?.id == tp.id,
                                    onClick = { selectedTopicId = tp.id },
                                    label = {
                                        val suffix = if (tp.statusEnum == ChapterStatus.NEEDS_ATTENTION) " (!)" else ""
                                        Text("${tp.name}$suffix")
                                    },
                                    modifier = Modifier.testTag("topic_chip_${tp.id}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Question Count, Difficulty, Types
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("3. Test Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // Question Count Stepper + Presets
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Number of Questions", style = MaterialTheme.typography.labelLarge)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { if (questionCount > 1) questionCount-- },
                                    enabled = questionCount > 1,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("decrease_question_count")
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease question count")
                                }
                                Text(
                                    text = "$questionCount",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(
                                    onClick = { if (questionCount < 20) questionCount++ },
                                    enabled = questionCount < 20,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("increase_question_count")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Increase question count")
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(3, 5, 8, 10, 15).forEach { countPreset ->
                                FilterChip(
                                    selected = questionCount == countPreset,
                                    onClick = { questionCount = countPreset },
                                    label = { Text("$countPreset Qs") }
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Difficulty Mix
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Difficulty Tier", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PracticeDifficultyFilter.entries.forEach { diff ->
                                FilterChip(
                                    selected = difficultyFilter == diff,
                                    onClick = { difficultyFilter = diff },
                                    label = { Text(diff.label) }
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Question Types
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Question Formats", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PracticeQuestionType.entries.forEach { qType ->
                                val isSelected = selectedTypes.contains(qType)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedTypes = if (isSelected && selectedTypes.size > 1) {
                                            selectedTypes - qType
                                        } else {
                                            selectedTypes + qType
                                        }
                                    },
                                    label = { Text(qType.label) }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    if (selectedChapter != null) {
                        onGenerateTest(selectedChapter, selectedTopic, questionCount, difficultyFilter, selectedTypes)
                    }
                },
                enabled = selectedChapter != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("generate_practice_test_button")
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = "Generate practice test")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate $questionCount Practice Question${if (questionCount == 1) "" else "s"}")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showChapterPicker) {
        ChapterPickerDialog(
            chapters = subjectChapters,
            selectedChapterId = selectedChapter?.id,
            onSelect = { ch ->
                selectedChapterId = ch.id
                selectedTopicId = null
                showChapterPicker = false
            },
            onDismiss = { showChapterPicker = false }
        )
    }
}

@Composable
private fun ChapterPickerDialog(
    chapters: List<Chapter>,
    selectedChapterId: String?,
    onSelect: (Chapter) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(chapters, query) {
        if (query.isBlank()) chapters
        else chapters.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Chapter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search chapter...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search chapters") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered, key = { it.id }) { ch ->
                        val isSelected = ch.id == selectedChapterId
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(ch) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ch.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = ch.classEnum.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusChip(ch.statusEnum)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun PracticeTestGeneratingContent(
    chapterName: String,
    topicName: String?,
    requestedCount: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Text(
                    text = "Crafting $requestedCount JEE Questions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (!topicName.isNullOrBlank()) "$chapterName • $topicName" else chapterName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Calibrating concepts, JEE Main & Advanced reasoning steps, and solutions...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PracticeTestRunnerContent(
    state: PracticeTestUiState.InProgress,
    onUpdateAnswer: (String, String) -> Unit,
    onGoToQuestion: (Int) -> Unit,
    onSubmitTest: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var elapsedSeconds by rememberSaveable(state.questions.firstOrNull()?.id) {
        mutableIntStateOf(state.elapsedSeconds)
    }
    var showSubmitConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.questions.firstOrNull()?.id) {
        while (true) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    val question = state.questions.getOrNull(state.currentIndex) ?: return
    val currentAnswer = state.userAnswers[question.id].orEmpty()
    val answeredCount = state.questions.count { !state.userAnswers[it.id].isNullOrBlank() }

    val mins = elapsedSeconds / 60
    val secs = elapsedSeconds % 60
    val timerText = "%02d:%02d".format(mins, secs)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Progress & Timer Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Question ${state.currentIndex + 1} of ${state.questions.size} • $answeredCount answered",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Rounded.Timer,
                        contentDescription = "Elapsed time",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(timerText, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        LinearProgressIndicator(
            progress = { (state.currentIndex + 1).toFloat() / state.questions.size.coerceAtLeast(1).toFloat() },
            modifier = Modifier.fillMaxWidth()
        )

        // Question Navigator Pills
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(state.questions) { idx, q ->
                val isCurrent = idx == state.currentIndex
                val isAnswered = !state.userAnswers[q.id].isNullOrBlank()
                val bg = when {
                    isCurrent -> MaterialTheme.colorScheme.primary
                    isAnswered -> MintEmerald.copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val fg = when {
                    isCurrent -> MaterialTheme.colorScheme.onPrimary
                    isAnswered -> MintEmerald
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(
                    shape = CircleShape,
                    color = bg,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onGoToQuestion(idx) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${idx + 1}",
                            color = fg,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Scrollable Question Body
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BadgePill(question.type.label, MaterialTheme.colorScheme.primary)
                            val diffColor = when (question.difficulty) {
                                PracticeDifficulty.EASY -> MintEmerald
                                PracticeDifficulty.MEDIUM -> SolarAmber
                                PracticeDifficulty.HARD -> AlertRose
                            }
                            BadgePill(question.difficulty.label, diffColor)
                            BadgePill(question.conceptTag, MaterialTheme.colorScheme.secondary)
                        }

                        Text(
                            text = LatexFormatter.formatForDisplay(question.prompt),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            item {
                when (question.type) {
                    PracticeQuestionType.MCQ -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            question.options.forEachIndexed { optIdx, option ->
                                val selected = currentAnswer == option
                                val optionLetter = ('A' + optIdx).toString()
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    border = BorderStroke(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 52.dp)
                                        .clickable { onUpdateAnswer(question.id, option) }
                                        .testTag("mcq_option_${question.id}_$optIdx")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        RadioButton(
                                            selected = selected,
                                            onClick = { onUpdateAnswer(question.id, option) }
                                        )
                                        Text(
                                            text = "$optionLetter. ${LatexFormatter.formatForDisplay(option)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    PracticeQuestionType.NUMERICAL -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = currentAnswer,
                                    onValueChange = { onUpdateAnswer(question.id, it) },
                                    label = { Text("Enter Numerical Value") },
                                    placeholder = { Text("e.g., 4.5 or -12") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("numerical_answer_input_${question.id}")
                                )
                                Text(
                                    text = "Tolerance applied automatically for rounded decimals.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    PracticeQuestionType.SHORT_ANSWER -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = currentAnswer,
                                    onValueChange = { onUpdateAnswer(question.id, it) },
                                    label = { Text("Your Short Answer / Expression") },
                                    placeholder = { Text("Write the key law, formula, or reasoning...") },
                                    minLines = 3,
                                    maxLines = 6,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("short_answer_input_${question.id}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Navigation & Submit Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onGoToQuestion(state.currentIndex - 1) },
                enabled = state.currentIndex > 0,
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Previous question", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Prev")
            }

            if (state.currentIndex < state.questions.lastIndex) {
                Button(
                    onClick = { onGoToQuestion(state.currentIndex + 1) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Next")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Next question", modifier = Modifier.size(18.dp))
                }
                OutlinedButton(
                    onClick = {
                        if (answeredCount < state.questions.size) showSubmitConfirm = true
                        else onSubmitTest(elapsedSeconds)
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("submit_practice_test_button")
                ) {
                    Text("Finish")
                }
            } else {
                Button(
                    onClick = {
                        if (answeredCount < state.questions.size) showSubmitConfirm = true
                        else onSubmitTest(elapsedSeconds)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("submit_practice_test_button")
                ) {
                    Text("Submit Test ($answeredCount/${state.questions.size})")
                }
            }
        }
    }

    if (showSubmitConfirm) {
        val unanswered = state.questions.size - answeredCount
        AlertDialog(
            onDismissRequest = { showSubmitConfirm = false },
            title = { Text("Submit Practice Test?") },
            text = {
                Text(
                    "You have $unanswered unanswered question${if (unanswered == 1) "" else "s"}. Submit now and evaluate your score?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitConfirm = false
                        onSubmitTest(elapsedSeconds)
                    }
                ) {
                    Text("Submit Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirm = false }) {
                    Text("Review Questions")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PracticeTestResultsContent(
    summary: PracticeTestOutcomeSummary,
    onSaveErrorToNotebook: (EvaluatedQuestionResult) -> Unit,
    onStartRevision: () -> Unit,
    onNewTest: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedQuestionIds = remember { mutableStateListOf<String>() }
    val scoreColor = when {
        summary.accuracyPercent >= 70.0 -> MintEmerald
        summary.accuracyPercent >= 45.0 -> SolarAmber
        else -> AlertRose
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Score Summary Hero
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = summary.topicName ?: summary.chapterName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (summary.topicName != null) {
                                Text(
                                    text = summary.chapterName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            color = scoreColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "${summary.accuracyPercent.roundToInt()}%",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Text(
                        text = "${summary.correctCount} of ${summary.totalQuestions} correct • ${summary.durationMinutes} min spent",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Revision Schedule Impact Card
        item {
            val impactAccent = if (summary.triggeredEarlierRevision) AlertRose else MintEmerald
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = impactAccent.copy(alpha = 0.11f)),
                border = BorderStroke(1.dp, impactAccent.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Schedule,
                            contentDescription = "Revision schedule update",
                            tint = impactAccent
                        )
                        Text(
                            text = if (summary.triggeredEarlierRevision) {
                                "Earlier Revision Triggered"
                            } else {
                                "Spaced Repetition Updated"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = impactAccent
                        )
                    }
                    Text(
                        text = "Next Review: ${IstDateUtils.formatDisplay(summary.nextRevisionDate)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = summary.rescheduleExplanation,
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (summary.weakConceptTags.isNotEmpty()) {
                        Text(
                            text = "Concepts to reinforce:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            summary.weakConceptTags.forEach { tag ->
                                BadgePill(tag, AlertRose)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (summary.triggeredEarlierRevision || summary.accuracyPercent < 70.0) {
                            Button(
                                onClick = onStartRevision,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("revise_weak_topic_now_button")
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Revise now", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Revise Now")
                            }
                        }
                        OutlinedButton(
                            onClick = onNewTest,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("New Test")
                        }
                    }
                }
            }
        }

        // Breakdown Header
        item {
            Text(
                text = "Question-by-Question Review",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        itemsIndexed(summary.questionResults, key = { _, res -> res.question.id }) { idx, res ->
            val q = res.question
            val statusColor = if (res.isCorrect) MintEmerald else AlertRose
            val alreadySaved = savedQuestionIds.contains(q.id)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (res.isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Close,
                                contentDescription = if (res.isCorrect) "Correct" else "Incorrect",
                                tint = statusColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Q${idx + 1} • ${q.type.label}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                        BadgePill(q.conceptTag, MaterialTheme.colorScheme.primary)
                    }

                    Text(
                        text = LatexFormatter.formatForDisplay(q.prompt),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Your Answer: ${res.userAnswer.ifBlank { "Unanswered" }.let { LatexFormatter.formatForDisplay(it) }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Correct Answer: ${LatexFormatter.formatForDisplay(q.correctAnswer)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MintEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = "Solution: ${LatexFormatter.formatForDisplay(q.solutionSteps)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!res.isCorrect) {
                        OutlinedButton(
                            onClick = {
                                if (!alreadySaved) {
                                    savedQuestionIds.add(q.id)
                                    onSaveErrorToNotebook(res)
                                }
                            },
                            enabled = !alreadySaved,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                Icons.Rounded.BookmarkAdd,
                                contentDescription = "Save to error notebook",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (alreadySaved) "Saved to Error Notebook" else "Save to Error Notebook")
                        }
                    }
                }
            }
        }

        item {
            TextButton(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Return to Workspace")
            }
        }
    }
}

@Composable
private fun BadgePill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
