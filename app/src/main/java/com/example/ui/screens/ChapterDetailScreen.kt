package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.engine.IstDateUtils
import com.example.domain.model.Chapter
import com.example.domain.model.ChapterStatus
import com.example.domain.model.Topic
import com.example.ui.components.MetricStatCard
import com.example.ui.components.RevisionHistoryDialog
import com.example.ui.components.StatusChip
import com.example.ui.components.SubjectBadge
import com.example.ui.viewmodel.JeeAppUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChapterDetailScreen(
    chapter: Chapter,
    uiState: JeeAppUiState,
    onBack: () -> Unit,
    onStartChapterRevision: () -> Unit,
    onStartTopicRevision: (Topic) -> Unit,
    onStartChapterPracticeTest: () -> Unit,
    onStartTopicPracticeTest: (Topic) -> Unit,
    onUpdateStatus: (ChapterStatus) -> Unit,
    onRenameChapter: (String) -> Unit,
    onSaveNotes: (String, String, String, String, Int) -> Unit,
    onAddTopic: (String) -> Unit,
    onUpdateTopicStatus: (Topic, ChapterStatus) -> Unit,
    onSwapTopicOrder: (Topic, Topic) -> Unit,
    onRenameTopic: (Topic, String) -> Unit
) {
    val topics = remember(uiState.topics, chapter.id) {
        uiState.topics.filter { it.chapterId == chapter.id && !it.isArchived }
            .sortedBy { it.orderIndex }
    }
    val chapterSessions = remember(uiState.sessions, chapter.id) {
        uiState.sessions.filter { it.chapterId == chapter.id }
    }
    val linkedTestResults = remember(uiState.testRecords, chapter.id) {
        uiState.testRecords.mapNotNull { tr ->
            tr.parsedChapterResults().firstOrNull { it.chapterId == chapter.id }?.let { tr.testName to it }
        }
    }

    var personalNotes by remember(chapter.id, chapter.personalNotes) { mutableStateOf(chapter.personalNotes) }
    var formulas by remember(chapter.id, chapter.importantFormulas) { mutableStateOf(chapter.importantFormulas) }
    var mistakes by remember(chapter.id, chapter.commonMistakes) { mutableStateOf(chapter.commonMistakes) }
    var diffQs by remember(chapter.id, chapter.difficultQuestions) { mutableStateOf(chapter.difficultQuestions) }
    var importance by remember(chapter.id, chapter.importance) { mutableIntStateOf(chapter.importance.toInt()) }

    var showRenameDialog by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var renameTopicTarget by remember { mutableStateOf<Topic?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(chapter.name, maxLines = 1)
                        Text(
                            text = "${chapter.subjectEnum.label} • ${chapter.classEnum.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to subjects")
                    }
                },
                actions = {
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Rename chapter")
                    }
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(Icons.Rounded.History, contentDescription = "View revision history")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Actions & Status
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SubjectBadge(chapter.subjectEnum)
                            StatusChip(chapter.statusEnum)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricStatCard(
                                title = "Revisions",
                                value = "${chapter.completedRevisions}",
                                subtitle = "Interval: ${chapter.currentIntervalDays}d",
                                modifier = Modifier.weight(1f)
                            )
                            MetricStatCard(
                                title = "Next Due",
                                value = IstDateUtils.formatShort(chapter.nextRevisionDate),
                                subtitle = "Last: ${IstDateUtils.formatShort(chapter.lastRevisionDate)}",
                                modifier = Modifier.weight(1f)
                            )
                            MetricStatCard(
                                title = "Accuracy",
                                value = if (chapter.totalQuestions > 0) "${chapter.accuracyPercent.roundToInt()}%" else "—",
                                subtitle = "${chapter.correctQuestions}/${chapter.totalQuestions} Qs",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = "Understanding: ${"%.1f".format(chapter.understandingRating)}/5 • Problem Solving: ${"%.1f".format(chapter.problemSolvingRating)}/5 • Confidence: ${"%.1f".format(chapter.confidenceRating)}/5",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (chapter.lastRescheduleReason.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Last schedule update: ${chapter.lastRescheduleReason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Button(
                            onClick = onStartChapterRevision,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("start_chapter_revision_button")
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Revise chapter")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Full Chapter Revision")
                        }
                        OutlinedButton(
                            onClick = onStartChapterPracticeTest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("start_chapter_practice_test_button")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = "AI practice test")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Take AI Practice Test")
                        }
                    }
                }
            }

            // Chapter Status Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Chapter Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ChapterStatus.entries.forEach { st ->
                            FilterChip(
                                selected = chapter.statusEnum == st,
                                onClick = { onUpdateStatus(st) },
                                label = { Text(st.label) }
                            )
                        }
                    }
                }
            }

            // Independent Topic-Level Tracking
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Topics (${topics.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Revise individual topics without revising the whole chapter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { showAddTopicDialog = true },
                        modifier = Modifier.testTag("add_topic_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add topic", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Topic")
                    }
                }
            }

            itemsIndexed(topics, key = { _, tp -> tp.id }) { idx, topic ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(topic.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                val subText = if (topic.nextRevisionDate.isNotBlank()) {
                                    "Next: ${IstDateUtils.formatShort(topic.nextRevisionDate)} (${topic.currentIntervalDays}d) • ${topic.completedRevisions} revs"
                                } else {
                                    "Not yet scheduled"
                                }
                                Text(subText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusChip(topic.statusEnum)
                        }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onStartTopicRevision(topic) },
                                modifier = Modifier.testTag("revise_topic_${topic.id}")
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Revise topic", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Revise")
                            }
                            OutlinedButton(
                                onClick = { onStartTopicPracticeTest(topic) },
                                modifier = Modifier.testTag("test_topic_${topic.id}")
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = "Test topic", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test")
                            }
                            if (topic.statusEnum == ChapterStatus.NOT_STARTED) {
                                TextButton(onClick = { onUpdateTopicStatus(topic, ChapterStatus.THEORY_COMPLETED) }) {
                                    Text("Start Cycle")
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { renameTopicTarget = topic }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Rename topic", modifier = Modifier.size(18.dp))
                            }
                            if (idx > 0) {
                                IconButton(onClick = { onSwapTopicOrder(topic, topics[idx - 1]) }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move topic up", modifier = Modifier.size(18.dp))
                                }
                            }
                            if (idx < topics.lastIndex) {
                                IconButton(onClick = { onSwapTopicOrder(topic, topics[idx + 1]) }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move topic down", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Linked Test Performance
            if (linkedTestResults.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Test Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            linkedTestResults.forEach { (testName, res) ->
                                Text(
                                    text = "$testName: ${res.accuracyPercent.roundToInt()}% accuracy (${res.questionsCorrect}/${res.questionsAttempted})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // Personal Notes, Formulas, Common Mistakes, Difficult Questions
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Chapter Study Vault", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = personalNotes,
                            onValueChange = { personalNotes = it },
                            label = { Text("Personal Notes") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = formulas,
                            onValueChange = { formulas = it },
                            label = { Text("Important Formulas & Conditions") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = mistakes,
                            onValueChange = { mistakes = it },
                            label = { Text("Common Mistakes") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = diffQs,
                            onValueChange = { diffQs = it },
                            label = { Text("Difficult Questions (Book / Sheet Refs)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Importance Rating: $importance / 5", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..5).forEach { v ->
                                FilterChip(selected = importance == v, onClick = { importance = v }, label = { Text("$v") })
                            }
                        }
                        Button(
                            onClick = { onSaveNotes(personalNotes, formulas, mistakes, diffQs, importance) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = "Save chapter profile")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Chapter Profile")
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(chapter.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Chapter") },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(onClick = { onRenameChapter(newName); showRenameDialog = false }, enabled = newName.isNotBlank()) {
                    Text("Save")
                }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAddTopicDialog) {
        var topicName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddTopicDialog = false },
            title = { Text("Add Topic to ${chapter.name}") },
            text = {
                OutlinedTextField(
                    value = topicName,
                    onValueChange = { topicName = it },
                    label = { Text("Topic Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { onAddTopic(topicName); showAddTopicDialog = false }, enabled = topicName.isNotBlank()) {
                    Text("Add")
                }
            },
            dismissButton = { TextButton(onClick = { showAddTopicDialog = false }) { Text("Cancel") } }
        )
    }

    renameTopicTarget?.let { target ->
        var newTopicName by remember { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { renameTopicTarget = null },
            title = { Text("Rename Topic") },
            text = {
                OutlinedTextField(value = newTopicName, onValueChange = { newTopicName = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(onClick = { onRenameTopic(target, newTopicName); renameTopicTarget = null }, enabled = newTopicName.isNotBlank()) {
                    Text("Save")
                }
            },
            dismissButton = { TextButton(onClick = { renameTopicTarget = null }) { Text("Cancel") } }
        )
    }

    if (showHistoryDialog) {
        RevisionHistoryDialog(
            title = chapter.name,
            sessions = chapterSessions,
            onDismiss = { showHistoryDialog = false }
        )
    }
}
