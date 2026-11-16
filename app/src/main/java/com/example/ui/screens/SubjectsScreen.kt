package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.domain.model.ChemistrySection
import com.example.domain.model.ClassLevel
import com.example.domain.model.Subject
import com.example.ui.components.StatusChip
import com.example.ui.viewmodel.JeeAppUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    uiState: JeeAppUiState,
    onOpenChapter: (String) -> Unit,
    onAddChapter: (String, Subject, ChemistrySection, ClassLevel, Int) -> Unit,
    onToggleArchive: (Chapter) -> Unit,
    onSwapOrder: (Chapter, Chapter) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubject by remember { mutableStateOf(Subject.PHYSICS) }
    var chemSection by remember { mutableStateOf(ChemistrySection.PHYSICAL) }
    var classFilter by remember { mutableStateOf<ClassLevel?>(null) }
    var showArchived by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val filteredChapters = remember(
        uiState.chapters, selectedSubject, chemSection, classFilter, showArchived, searchQuery
    ) {
        uiState.chapters.filter { ch ->
            ch.subjectEnum == selectedSubject &&
                (selectedSubject != Subject.CHEMISTRY || ch.sectionEnum == chemSection || ch.section.isBlank()) &&
                (classFilter == null || ch.classEnum == classFilter) &&
                (showArchived || !ch.isArchived) &&
                (searchQuery.isBlank() || ch.name.contains(searchQuery, ignoreCase = true))
        }.sortedWith(compareBy<Chapter> { it.isArchived }.thenBy { it.orderIndex }.thenBy { it.name })
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_chapter_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add chapter")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SecondaryTabRow(selectedTabIndex = selectedSubject.ordinal) {
                Subject.entries.forEach { subj ->
                    Tab(
                        selected = selectedSubject == subj,
                        onClick = { selectedSubject = subj },
                        text = {
                            Text(
                                text = subj.label,
                                color = if (selectedSubject == subj) subj.accentColor(isDark) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            if (selectedSubject == Subject.CHEMISTRY) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(ChemistrySection.PHYSICAL, ChemistrySection.INORGANIC, ChemistrySection.ORGANIC).forEach { sec ->
                        FilterChip(
                            selected = chemSection == sec,
                            onClick = { chemSection = sec },
                            label = { Text(sec.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search ${selectedSubject.label} chapters...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search chapters") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = classFilter == null,
                        onClick = { classFilter = null },
                        label = { Text("All Classes") }
                    )
                    FilterChip(
                        selected = classFilter == ClassLevel.CLASS_11,
                        onClick = { classFilter = ClassLevel.CLASS_11 },
                        label = { Text("Class 11") }
                    )
                    FilterChip(
                        selected = classFilter == ClassLevel.CLASS_12,
                        onClick = { classFilter = ClassLevel.CLASS_12 },
                        label = { Text("Class 12") }
                    )
                    FilterChip(
                        selected = showArchived,
                        onClick = { showArchived = !showArchived },
                        label = { Text("Archived") }
                    )
                }
                Text(
                    text = "Syllabus: ${uiState.userProfile?.syllabusVersion ?: "JEE-2026-v1"} • ${filteredChapters.size} chapters",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(filteredChapters, key = { _, ch -> ch.id }) { idx, chapter ->
                    val topicCount = uiState.topics.count { it.chapterId == chapter.id && !it.isArchived }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenChapter(chapter.id) }
                            .testTag("chapter_card_${chapter.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = chapter.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${chapter.classEnum.label} • $topicCount topics • ${chapter.completedRevisions} revisions",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusChip(chapter.statusEnum)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val nextLabel = if (chapter.nextRevisionDate.isNotBlank()) {
                                    "Next: ${IstDateUtils.formatShort(chapter.nextRevisionDate)} (${chapter.currentIntervalDays}d)"
                                } else {
                                    "Not yet scheduled"
                                }
                                val retLabel = if (chapter.completedRevisions > 0 || chapter.estimatedRetention > 0) {
                                    " • Est. ${chapter.estimatedRetention.roundToInt()}%"
                                } else ""
                                Text(
                                    text = nextLabel + retLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row {
                                    if (idx > 0) {
                                        IconButton(
                                            onClick = { onSwapOrder(chapter, filteredChapters[idx - 1]) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move chapter up", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    if (idx < filteredChapters.lastIndex) {
                                        IconButton(
                                            onClick = { onSwapOrder(chapter, filteredChapters[idx + 1]) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move chapter down", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    IconButton(
                                        onClick = { onToggleArchive(chapter) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (chapter.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                                            contentDescription = if (chapter.isArchived) "Unarchive chapter" else "Archive chapter",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddChapterDialog(
            initialSubject = selectedSubject,
            initialSection = chemSection,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, subj, sec, cls, imp ->
                onAddChapter(name, subj, sec, cls, imp)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddChapterDialog(
    initialSubject: Subject,
    initialSection: ChemistrySection,
    onDismiss: () -> Unit,
    onConfirm: (String, Subject, ChemistrySection, ClassLevel, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf(initialSubject) }
    var section by remember { mutableStateOf(initialSection) }
    var classLevel by remember { mutableStateOf(ClassLevel.CLASS_11) }
    var importance by remember { mutableIntStateOf(4) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Chapter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Chapter Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_chapter_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Subject.entries.forEach { s ->
                        FilterChip(
                            selected = subject == s,
                            onClick = { subject = s },
                            label = { Text(s.shortLabel) }
                        )
                    }
                }
                if (subject == Subject.CHEMISTRY) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(ChemistrySection.PHYSICAL, ChemistrySection.INORGANIC, ChemistrySection.ORGANIC).forEach { sec ->
                            FilterChip(
                                selected = section == sec,
                                onClick = { section = sec },
                                label = { Text(sec.name.take(4)) }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ClassLevel.entries.forEach { cls ->
                        FilterChip(
                            selected = classLevel == cls,
                            onClick = { classLevel = cls },
                            label = { Text(cls.label) }
                        )
                    }
                }
                Text("JEE Weightage / Importance: $importance / 5", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { imp ->
                        FilterChip(
                            selected = importance == imp,
                            onClick = { importance = imp },
                            label = { Text("$imp") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, subject, section, classLevel, importance) },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.testTag("confirm_add_chapter_button")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
