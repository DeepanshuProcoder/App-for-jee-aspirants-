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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.domain.model.ErrorCategory
import com.example.domain.model.ErrorRecord
import com.example.domain.model.ReattemptStatus
import com.example.domain.model.Subject
import com.example.ui.components.SubjectBadge
import com.example.ui.theme.AlertRose
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.SolarAmber
import com.example.ui.viewmodel.JeeAppUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ErrorNotebookScreen(
    uiState: JeeAppUiState,
    onAddError: (Subject, Chapter?, String, String, ErrorCategory, String, String, String) -> Unit,
    onScheduleErrorRevision: (ErrorRecord) -> Unit,
    onUpdateErrorStatus: (ErrorRecord, ReattemptStatus) -> Unit,
    onDeleteError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var subjectFilter by remember { mutableStateOf<Subject?>(null) }
    var categoryFilter by remember { mutableStateOf<ErrorCategory?>(null) }
    var statusFilter by remember { mutableStateOf<ReattemptStatus?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = remember(uiState.errorRecords, searchQuery, subjectFilter, categoryFilter, statusFilter) {
        uiState.errorRecords.filter { e ->
            (subjectFilter == null || e.subjectEnum == subjectFilter) &&
                (categoryFilter == null || e.categoryEnum == categoryFilter) &&
                (statusFilter == null || e.statusEnum == statusFilter) &&
                (searchQuery.isBlank() ||
                    e.questionRef.contains(searchQuery, true) ||
                    e.chapterName.contains(searchQuery, true) ||
                    e.mistakeExplanation.contains(searchQuery, true) ||
                    e.importantLesson.contains(searchQuery, true))
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_error_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Log error")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Error Notebook", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search mistakes, chapters, book refs...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search errors") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(selected = subjectFilter == null, onClick = { subjectFilter = null }, label = { Text("All") })
                        Subject.entries.forEach { s ->
                            FilterChip(
                                selected = subjectFilter == s,
                                onClick = { subjectFilter = if (subjectFilter == s) null else s },
                                label = { Text(s.shortLabel) }
                            )
                        }
                        ReattemptStatus.entries.forEach { st ->
                            FilterChip(
                                selected = statusFilter == st,
                                onClick = { statusFilter = if (statusFilter == st) null else st },
                                label = { Text(st.label) }
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ErrorCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = categoryFilter == cat,
                                onClick = { categoryFilter = if (categoryFilter == cat) null else cat },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("No error entries match", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Log conceptual, application, calculation, or time-management mistakes so they never repeat in tests.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { err ->
                    val statusColor = when (err.statusEnum) {
                        ReattemptStatus.SOLVED_CORRECTLY -> MintEmerald
                        ReattemptStatus.SCHEDULED -> MaterialTheme.colorScheme.primary
                        ReattemptStatus.NEEDS_REVIEW -> AlertRose
                        ReattemptStatus.PENDING -> SolarAmber
                    }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    SubjectBadge(err.subjectEnum)
                                    Surface(color = statusColor.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = err.statusEnum.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = IstDateUtils.formatShort(err.dateRecorded),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "${err.chapterName}${if (err.topicName.isNotBlank()) " • ${err.topicName}" else ""}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${err.categoryEnum.label} • ${err.questionRef}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (err.mistakeExplanation.isNotBlank()) {
                                Text("Mistake: ${err.mistakeExplanation}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (err.correctApproach.isNotBlank()) {
                                Text("Correct: ${err.correctApproach}", style = MaterialTheme.typography.bodySmall, color = MintEmerald)
                            }
                            if (err.importantLesson.isNotBlank()) {
                                Text("Lesson: ${err.importantLesson}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onScheduleErrorRevision(err) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Schedule, contentDescription = "Schedule error for revision")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Schedule Revision")
                                }
                                IconButton(
                                    onClick = { onUpdateErrorStatus(err, ReattemptStatus.SOLVED_CORRECTLY) }
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Mark solved correctly", tint = MintEmerald)
                                }
                                IconButton(onClick = { onDeleteError(err.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete error")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddErrorDialog(
            chapters = uiState.chapters.filter { !it.isArchived },
            onDismiss = { showAddDialog = false },
            onConfirm = { subj, ch, topic, qRef, cat, mistake, correct, lesson ->
                onAddError(subj, ch, topic, qRef, cat, mistake, correct, lesson)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddErrorDialog(
    chapters: List<Chapter>,
    onDismiss: () -> Unit,
    onConfirm: (Subject, Chapter?, String, String, ErrorCategory, String, String, String) -> Unit
) {
    var subject by remember { mutableStateOf(Subject.PHYSICS) }
    val subjChapters = remember(chapters, subject) { chapters.filter { it.subjectEnum == subject } }
    var selectedChapter by remember(subject) { mutableStateOf(subjChapters.firstOrNull()) }
    var topicName by remember { mutableStateOf("") }
    var questionRef by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ErrorCategory.CONCEPTUAL) }
    var mistake by remember { mutableStateOf("") }
    var correct by remember { mutableStateOf("") }
    var lesson by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Error Entry") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Subject.entries.forEach { s ->
                        FilterChip(selected = subject == s, onClick = { subject = s }, label = { Text(s.shortLabel) })
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    subjChapters.take(10).forEach { ch ->
                        FilterChip(
                            selected = selectedChapter?.id == ch.id,
                            onClick = { selectedChapter = ch },
                            label = { Text(ch.name, maxLines = 1) }
                        )
                    }
                }
                OutlinedTextField(value = topicName, onValueChange = { topicName = it }, label = { Text("Topic (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = questionRef, onValueChange = { questionRef = it }, label = { Text("Question Ref / Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ErrorCategory.entries.forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) })
                    }
                }
                OutlinedTextField(value = mistake, onValueChange = { mistake = it }, label = { Text("Explanation of Mistake") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = correct, onValueChange = { correct = it }, label = { Text("Correct Approach") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = lesson, onValueChange = { lesson = it }, label = { Text("Important Lesson") }, maxLines = 2, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(subject, selectedChapter, topicName, questionRef, category, mistake, correct, lesson) },
                enabled = questionRef.isNotBlank()
            ) {
                Text("Save Error")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
