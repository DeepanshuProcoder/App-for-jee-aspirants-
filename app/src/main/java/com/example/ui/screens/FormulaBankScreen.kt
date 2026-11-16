package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.engine.LatexFormatter
import com.example.domain.model.Chapter
import com.example.domain.model.NoteType
import com.example.domain.model.Subject
import com.example.ui.theme.MonoFontFamily
import com.example.ui.viewmodel.JeeAppUiState

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FormulaBankScreen(
    uiState: JeeAppUiState,
    onAddNote: (Subject, Chapter?, NoteType, String, String, String, String) -> Unit,
    onDeleteNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubject by remember { mutableStateOf(Subject.PHYSICS) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedChapterId by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val subjectChapters = remember(uiState.chapters, selectedSubject) {
        uiState.chapters.filter { !it.isArchived && it.subjectEnum == selectedSubject }
    }
    val filteredNotes = remember(uiState.formulaNotes, selectedSubject, selectedChapterId, searchQuery) {
        uiState.formulaNotes.filter { note ->
            note.subjectEnum == selectedSubject &&
                (selectedChapterId == null || note.chapterId == selectedChapterId) &&
                (searchQuery.isBlank() ||
                    note.title.contains(searchQuery, true) ||
                    note.content.contains(searchQuery, true) ||
                    note.chapterName.contains(searchQuery, true))
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_formula_note_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add formula or note")
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
                        onClick = {
                            selectedSubject = subj
                            selectedChapterId = null
                        },
                        text = {
                            Text(
                                text = subj.label,
                                color = if (selectedSubject == subj) subj.accentColor(isDark) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search ${selectedSubject.label} formulas, reactions, tricks...") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search notes") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = selectedChapterId == null,
                                onClick = { selectedChapterId = null },
                                label = { Text("All Chapters") }
                            )
                            subjectChapters.take(8).forEach { ch ->
                                FilterChip(
                                    selected = selectedChapterId == ch.id,
                                    onClick = { selectedChapterId = if (selectedChapterId == ch.id) null else ch.id },
                                    label = { Text(ch.name, maxLines = 1) }
                                )
                            }
                        }
                    }
                }

                if (filteredNotes.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Empty ${selectedSubject.label} Bank", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Save formulas, derivations, named reactions, exceptions, LaTeX equations & photo snapshots. Available right inside your Revision Session!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredNotes, key = { it.id }) { note ->
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${note.noteTypeEnum.label} • ${note.chapterName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = selectedSubject.accentColor(isDark)
                                        )
                                        Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(onClick = { onDeleteNote(note.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete note")
                                    }
                                }

                                if (note.latexFormula.isNotBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = LatexFormatter.formatForDisplay(note.latexFormula),
                                            fontFamily = MonoFontFamily,
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }

                                if (note.content.isNotBlank()) {
                                    Text(note.content, style = MaterialTheme.typography.bodyMedium)
                                }

                                if (note.attachmentUri.isNotBlank()) {
                                    AsyncImage(
                                        model = note.attachmentUri,
                                        contentDescription = "Note attachment for ${note.title}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFormulaNoteDialog(
            initialSubject = selectedSubject,
            chapters = uiState.chapters.filter { !it.isArchived },
            onDismiss = { showAddDialog = false },
            onConfirm = { subj, ch, type, title, content, latex, uri ->
                onAddNote(subj, ch, type, title, content, latex, uri)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddFormulaNoteDialog(
    initialSubject: Subject,
    chapters: List<Chapter>,
    onDismiss: () -> Unit,
    onConfirm: (Subject, Chapter?, NoteType, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var subject by remember { mutableStateOf(initialSubject) }
    val subjChapters = remember(chapters, subject) { chapters.filter { it.subjectEnum == subject } }
    var selectedChapter by remember(subject) { mutableStateOf(subjChapters.firstOrNull()) }
    var noteType by remember { mutableStateOf(NoteType.FORMULA) }
    var title by remember { mutableStateOf("") }
    var latex by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf("") }

    // Zero-permission Android Photo Picker per Google Play Policy
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Non-persistable URI still readable for current session
            }
            attachmentUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Formula Bank") },
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
                    subjChapters.take(8).forEach { ch ->
                        FilterChip(
                            selected = selectedChapter?.id == ch.id,
                            onClick = { selectedChapter = ch },
                            label = { Text(ch.name, maxLines = 1) }
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NoteType.entries.forEach { nt ->
                        FilterChip(selected = noteType == nt, onClick = { noteType = nt }, label = { Text(nt.label) })
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = latex,
                    onValueChange = { latex = it },
                    label = { Text("LaTeX Equation (e.g., F = \\mu N, \\Delta G = \\Delta H - T\\Delta S)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                if (latex.isNotBlank()) {
                    Text(
                        text = "Preview: ${LatexFormatter.formatForDisplay(latex)}",
                        fontFamily = MonoFontFamily,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Explanation / Conditions / Exceptions") }, maxLines = 3, modifier = Modifier.fillMaxWidth())

                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = "Attach diagram or photo")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (attachmentUri.isBlank()) "Attach Photo (Photo Picker)" else "Photo Attached ✓")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(subject, selectedChapter, noteType, title, content, latex, attachmentUri) },
                enabled = title.isNotBlank()
            ) {
                Text("Save Note")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
