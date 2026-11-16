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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Schedule
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.engine.IstDateUtils
import com.example.domain.model.Chapter
import com.example.domain.model.TestChapterResult
import com.example.domain.model.TestType
import com.example.ui.components.MultiSeriesTestPoint
import com.example.ui.components.TestTrendChart
import com.example.ui.theme.AlertRose
import com.example.ui.theme.ChemistryColor
import com.example.ui.theme.MathsColor
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.PhysicsColor
import com.example.ui.viewmodel.JeeAppUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TestAnalysisScreen(
    uiState: JeeAppUiState,
    onAddTest: (
        testName: String,
        testDate: String,
        testType: TestType,
        physicsScore: Double,
        physicsMax: Double,
        chemistryScore: Double,
        chemistryMax: Double,
        mathsScore: Double,
        mathsMax: Double,
        accuracyPercent: Double,
        timeNotes: String,
        chapterResults: List<TestChapterResult>
    ) -> Unit,
    onDeleteTest: (String) -> Unit,
    onScheduleExtraRevision: (String, String) -> Unit,
    onOpenPracticeTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    val chartPoints = remember(uiState.testRecords) {
        uiState.testRecords.sortedBy { it.testDate }.takeLast(8).map { t ->
            MultiSeriesTestPoint(
                label = IstDateUtils.formatShort(t.testDate),
                physicsPct = if (t.physicsMax > 0) ((t.physicsScore / t.physicsMax) * 100.0).toFloat() else 0f,
                chemistryPct = if (t.chemistryMax > 0) ((t.chemistryScore / t.chemistryMax) * 100.0).toFloat() else 0f,
                mathsPct = if (t.mathsMax > 0) ((t.mathsScore / t.mathsMax) * 100.0).toFloat() else 0f,
                totalPct = if (t.maxMarks > 0) ((t.totalMarks / t.maxMarks) * 100.0).toFloat() else 0f
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_test_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Log test result")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Test Analysis", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Tracks score trends & chapter gaps over time (individual tests never predict final JEE rank).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onOpenPracticeTest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_ai_practice_test_button")
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = "Take AI chapter test")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Take AI Chapter / Topic Test")
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Score Trajectory (%)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TestTrendChart(points = chartPoints)
                    }
                }
            }

            if (uiState.testRecords.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("No tests logged yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Tap + to enter an ALLEN Minor/Major or JEE mock test. Weak chapters are automatically connected to your revision queue.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.testRecords, key = { it.id }) { test ->
                    val chapterResults = test.parsedChapterResults()
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
                                    Text(test.testName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${test.testTypeEnum.label} • ${IstDateUtils.formatDisplay(test.testDate)} • ${test.accuracyPercent.roundToInt()}% acc",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteTest(test.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete test record")
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ScorePill("PHY", test.physicsScore, test.physicsMax, PhysicsColor, Modifier.weight(1f))
                                ScorePill("CHEM", test.chemistryScore, test.chemistryMax, ChemistryColor, Modifier.weight(1f))
                                ScorePill("MATH", test.mathsScore, test.mathsMax, MathsColor, Modifier.weight(1f))
                                ScorePill("TOTAL", test.totalMarks, test.maxMarks, MintEmerald, Modifier.weight(1f))
                            }

                            if (test.timeManagementNotes.isNotBlank()) {
                                Text(
                                    text = "Time Notes: ${test.timeManagementNotes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            chapterResults.forEach { res ->
                                val isWeak = res.weakFlag || res.accuracyPercent < 60.0
                                Surface(
                                    color = if (isWeak) AlertRose.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(res.chapterName, style = MaterialTheme.typography.labelLarge)
                                            Text(
                                                text = "${res.accuracyPercent.roundToInt()}% accuracy",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isWeak) AlertRose else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isWeak && res.chapterId.isNotBlank()) {
                                            OutlinedButton(
                                                onClick = {
                                                    onScheduleExtraRevision(
                                                        res.chapterId,
                                                        "Weak in ${test.testName} (${res.accuracyPercent.roundToInt()}%)"
                                                    )
                                                }
                                            ) {
                                                Icon(Icons.Rounded.Schedule, contentDescription = "Schedule extra revision")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Revise")
                                            }
                                        }
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
        AddTestDialog(
            chapters = uiState.chapters.filter { !it.isArchived },
            onDismiss = { showAddDialog = false },
            onConfirm = { name, date, type, phy, pMax, chem, cMax, math, mMax, acc, notes, chRes ->
                onAddTest(name, date, type, phy, pMax, chem, cMax, math, mMax, acc, notes, chRes)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ScorePill(
    label: String,
    score: Double,
    maxScore: Double,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text("${score.toInt()}/${maxScore.toInt()}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddTestDialog(
    chapters: List<Chapter>,
    onDismiss: () -> Unit,
    onConfirm: (
        String, String, TestType,
        Double, Double, Double, Double, Double, Double, Double,
        String, List<TestChapterResult>
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(IstDateUtils.todayIst()) }
    var testType by remember { mutableStateOf(TestType.ALLEN_MINOR) }
    var phy by remember { mutableStateOf("65") }
    var phyMax by remember { mutableStateOf("100") }
    var chem by remember { mutableStateOf("72") }
    var chemMax by remember { mutableStateOf("100") }
    var math by remember { mutableStateOf("58") }
    var mathMax by remember { mutableStateOf("100") }
    var accuracy by remember { mutableStateOf("78") }
    var timeNotes by remember { mutableStateOf("") }

    val flaggedChapters = remember { mutableStateListOf<Chapter>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Test Analysis") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Test Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TestType.entries.forEach { t ->
                        FilterChip(selected = testType == t, onClick = { testType = t }, label = { Text(t.label) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = phy, onValueChange = { phy = it }, label = { Text("Phy") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = phyMax, onValueChange = { phyMax = it }, label = { Text("Max") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = chem, onValueChange = { chem = it }, label = { Text("Chem") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = chemMax, onValueChange = { chemMax = it }, label = { Text("Max") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = math, onValueChange = { math = it }, label = { Text("Math") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = mathMax, onValueChange = { mathMax = it }, label = { Text("Max") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = accuracy, onValueChange = { accuracy = it }, label = { Text("Overall Accuracy %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = timeNotes, onValueChange = { timeNotes = it }, label = { Text("Time Management Notes") }, maxLines = 2, modifier = Modifier.fillMaxWidth())

                Text("Flag Weak Chapters for Revision:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    chapters.take(15).forEach { ch ->
                        val isSel = flaggedChapters.any { it.id == ch.id }
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                if (isSel) flaggedChapters.removeAll { it.id == ch.id } else flaggedChapters.add(ch)
                            },
                            label = { Text(ch.name, maxLines = 1) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val chResults = flaggedChapters.map { ch ->
                        TestChapterResult(
                            chapterId = ch.id,
                            chapterName = ch.name,
                            subject = ch.subject,
                            accuracyPercent = 45.0,
                            weakFlag = true
                        )
                    }
                    onConfirm(
                        name, date, testType,
                        phy.toDoubleOrNull() ?: 0.0, phyMax.toDoubleOrNull() ?: 100.0,
                        chem.toDoubleOrNull() ?: 0.0, chemMax.toDoubleOrNull() ?: 100.0,
                        math.toDoubleOrNull() ?: 0.0, mathMax.toDoubleOrNull() ?: 100.0,
                        accuracy.toDoubleOrNull() ?: 0.0,
                        timeNotes, chResults
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save Test")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
