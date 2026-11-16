package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.domain.engine.LatexFormatter
import com.example.domain.model.Chapter
import com.example.domain.model.ErrorCategory
import com.example.domain.model.FormulaNote
import com.example.domain.model.RevisionOutcome
import com.example.domain.model.ScheduleComputationResult
import com.example.domain.model.Topic
import com.example.ui.components.SubjectBadge
import com.example.ui.theme.AlertRose
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SolarAmber
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RevisionSessionScreen(
    chapter: Chapter,
    topic: Topic?,
    chapterNotes: List<FormulaNote>,
    previewSchedule: (
        outcome: RevisionOutcome,
        understanding: Int,
        confidence: Int,
        difficulty: Int,
        attempted: Int,
        correct: Int,
        hints: Int,
        unsolved: Int,
        durationMinutes: Int
    ) -> ScheduleComputationResult,
    onSubmitSession: (
        durationMinutes: Int,
        activeRecallNotes: String,
        theoryChecklist: List<String>,
        attempted: Int,
        correct: Int,
        incorrect: Int,
        hints: Int,
        unsolved: Int,
        whatForgot: String,
        unclearConcept: String,
        mistakeMade: String,
        needsPractice: String,
        understanding: Int,
        confidence: Int,
        difficulty: Int,
        outcome: RevisionOutcome,
        saveToErrorNotebook: Boolean,
        errorCategory: ErrorCategory,
        onResult: (ScheduleComputationResult) -> Unit
    ) -> Unit,
    onClose: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var showFormulaSheet by remember { mutableStateOf(false) }
    var savedResult by remember { mutableStateOf<ScheduleComputationResult?>(null) }

    // Step 1
    var activeRecallNotes by remember { mutableStateOf("") }

    // Step 2
    val theoryOptions = listOf("Definitions", "Formulas", "Derivations", "Important Concepts", "Conditions & Exceptions")
    var checkedTheory by remember { mutableStateOf(setOf("Definitions", "Formulas")) }

    // Step 3
    var attemptedStr by remember { mutableStateOf("10") }
    var correctStr by remember { mutableStateOf("8") }
    var incorrectStr by remember { mutableStateOf("1") }
    var hintsStr by remember { mutableStateOf("1") }
    var unsolvedStr by remember { mutableStateOf("1") }

    // Step 4
    var whatForgot by remember { mutableStateOf("") }
    var unclearConcept by remember { mutableStateOf("") }
    var mistakeMade by remember { mutableStateOf("") }
    var needsPractice by remember { mutableStateOf("") }
    var saveToErrorNotebook by remember { mutableStateOf(false) }
    var errorCategory by remember { mutableStateOf(ErrorCategory.CONCEPTUAL) }

    // Step 5
    var understanding by remember { mutableIntStateOf(4) }
    var confidence by remember { mutableIntStateOf(4) }
    var difficulty by remember { mutableIntStateOf(3) }
    var outcome by remember { mutableStateOf(RevisionOutcome.GOOD) }

    LaunchedEffect(savedResult) {
        if (savedResult == null) {
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    val durationMinutes = (elapsedSeconds / 60).coerceAtLeast(1)
    val attempted = attemptedStr.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val correct = correctStr.toIntOrNull()?.coerceIn(0, attempted.coerceAtLeast(0)) ?: 0
    val incorrect = incorrectStr.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val hints = hintsStr.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val unsolved = unsolvedStr.toIntOrNull()?.coerceAtLeast(0) ?: 0

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = topic?.name ?: chapter.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Text(
                            text = if (topic != null) "${chapter.name} • Step $step of 5" else "Full Chapter • Step $step of 5",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = "Exit revision session")
                    }
                },
                actions = {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Timer, contentDescription = "Elapsed timer", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (savedResult != null) {
            val res = savedResult!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Session saved", tint = MintEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Revision Recorded & Rescheduled",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        SubjectBadge(chapter.subjectEnum)
                        Text(
                            text = "Next Revision: ${IstDateUtils.formatDisplay(res.nextRevisionDate)} (+${res.nextIntervalDays} days)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Updated Status: ${res.newStatus.label} • Est. Retention: ${res.estimatedRetentionAfter.toInt()}%",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Why this interval?\n${res.explanation}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (res.recommendedAction.isNotBlank()) {
                                    Text(
                                        text = "Next Action: ${res.recommendedAction}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = onClose,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("finish_session_done_button")
                        ) {
                            Text("Back to Queue")
                        }
                    }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LinearProgressIndicator(
                progress = { step / 5f },
                modifier = Modifier.fillMaxWidth()
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (step) {
                    1 -> {
                        Text(
                            text = "STEP 1: ACTIVE RECALL",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Close your notebook and try recalling key concepts, laws, reactions, and formulas from memory first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = activeRecallNotes,
                            onValueChange = { activeRecallNotes = it },
                            label = { Text("Brain-dump recalled points & formulas") },
                            minLines = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("step1_recall_input")
                        )
                    }

                    2 -> {
                        Text(
                            text = "STEP 2: THEORY RECALL",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Check off the theory components you successfully verified.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { showFormulaSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_formula_sheet_button")
                        ) {
                            Icon(Icons.Rounded.AutoStories, contentDescription = "Open Formula Bank")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Review Formula Bank & Chapter Notes (${chapterNotes.size})")
                        }
                        theoryOptions.forEach { item ->
                            val isChecked = checkedTheory.contains(item)
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = {
                                            checkedTheory = if (isChecked) checkedTheory - item else checkedTheory + item
                                        }
                                    )
                                    Text(item, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                        if (chapter.importantFormulas.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Chapter Quick Formulas", style = MaterialTheme.typography.labelLarge)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(chapter.importantFormulas, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    3 -> {
                        Text(
                            text = "STEP 3: PROBLEM SOLVING",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Record your practice problem numbers honestly — accuracy directly calibrates your next interval.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NumberField("Attempted", attemptedStr, { attemptedStr = it }, Modifier.weight(1f))
                            NumberField("Correct", correctStr, { correctStr = it }, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NumberField("Incorrect", incorrectStr, { incorrectStr = it }, Modifier.weight(1f))
                            NumberField("Used Hints", hintsStr, { hintsStr = it }, Modifier.weight(1f))
                        }
                        NumberField("Left Unsolved", unsolvedStr, { unsolvedStr = it }, Modifier.fillMaxWidth())

                        val accPct = if (attempted > 0) (correct * 100) / attempted else 0
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Calculated Accuracy: $accPct% ($correct / $attempted)",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    4 -> {
                        Text(
                            text = "STEP 4: ERROR ANALYSIS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = whatForgot,
                            onValueChange = { whatForgot = it },
                            label = { Text("What did I forget?") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                        OutlinedTextField(
                            value = unclearConcept,
                            onValueChange = { unclearConcept = it },
                            label = { Text("What concept was unclear?") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                        OutlinedTextField(
                            value = mistakeMade,
                            onValueChange = { mistakeMade = it },
                            label = { Text("What mistake did I make?") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                        OutlinedTextField(
                            value = needsPractice,
                            onValueChange = { needsPractice = it },
                            label = { Text("What needs further practice?") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = saveToErrorNotebook,
                                onCheckedChange = { saveToErrorNotebook = it }
                            )
                            Text("Also log mistake to Error Notebook", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (saveToErrorNotebook) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ErrorCategory.entries.forEach { cat ->
                                    FilterChip(
                                        selected = errorCategory == cat,
                                        onClick = { errorCategory = cat },
                                        label = { Text(cat.label) }
                                    )
                                }
                            }
                        }
                    }

                    5 -> {
                        Text(
                            text = "STEP 5: SELF-ASSESSMENT",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        RatingSelector("Understanding (1–5)", understanding) { understanding = it }
                        RatingSelector("Confidence (1–5)", confidence) { confidence = it }
                        RatingSelector("Difficulty (1–5)", difficulty) { difficulty = it }

                        Text("Select Revision Outcome", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            RevisionOutcome.entries.forEach { opt ->
                                val selected = outcome == opt
                                val accent = when (opt) {
                                    RevisionOutcome.EASY, RevisionOutcome.GOOD -> MintEmerald
                                    RevisionOutcome.DIFFICULT -> SolarAmber
                                    RevisionOutcome.FORGOT -> AlertRose
                                }
                                Surface(
                                    onClick = { outcome = opt },
                                    color = if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("outcome_${opt.name.lowercase()}")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = opt.label,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = opt.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        val preview = previewSchedule(
                            outcome, understanding, confidence, difficulty,
                            attempted, correct, hints, unsolved, durationMinutes
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Preview Next Review: ${IstDateUtils.formatDisplay(preview.nextRevisionDate)} (${preview.nextIntervalDays}d)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(preview.explanation, style = MaterialTheme.typography.bodySmall)
                                if (preview.recommendedAction.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = preview.recommendedAction,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Previous step")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Back")
                    }
                }
                Button(
                    onClick = {
                        if (step < 5) {
                            step++
                        } else {
                            onSubmitSession(
                                durationMinutes,
                                activeRecallNotes,
                                checkedTheory.toList(),
                                attempted,
                                correct,
                                incorrect,
                                hints,
                                unsolved,
                                whatForgot,
                                unclearConcept,
                                mistakeMade,
                                needsPractice,
                                understanding,
                                confidence,
                                difficulty,
                                outcome,
                                saveToErrorNotebook,
                                errorCategory
                            ) { result ->
                                savedResult = result
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(50.dp)
                        .testTag(if (step < 5) "session_next_step_button" else "save_revision_session_button")
                ) {
                    Text(if (step < 5) "Next Step" else "Save & Schedule")
                }
            }
        }
    }

    if (showFormulaSheet) {
        ModalBottomSheet(onDismissRequest = { showFormulaSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${chapter.name} • Formula & Note Bank",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (chapterNotes.isEmpty() && chapter.importantFormulas.isBlank()) {
                    Text(
                        text = "No notes saved for this chapter yet. Add formulas or tricks in the Formula Bank or Chapter Profile.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    if (chapter.importantFormulas.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Chapter Profile Formulas", style = MaterialTheme.typography.labelLarge)
                                Text(chapter.importantFormulas, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    chapterNotes.forEach { note ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${note.noteTypeEnum.label}: ${note.title}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                if (note.latexFormula.isNotBlank()) {
                                    Text(
                                        text = LatexFormatter.formatForDisplay(note.latexFormula),
                                        fontFamily = MonoFontFamily,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (note.content.isNotBlank()) {
                                    Text(note.content, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { s -> onValueChange(s.filter { it.isDigit() }.take(4)) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun RatingSelector(label: String, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (1..5).forEach { value ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text("$value") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
