package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.engine.IstDateUtils
import com.example.domain.model.ChapterStatus
import com.example.domain.model.RevisionOutcome
import com.example.domain.model.RevisionSession
import com.example.domain.model.Subject
import com.example.ui.theme.AlertRose
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.SolarAmber

@Composable
fun SubjectBadge(subject: Subject, modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val color = subject.accentColor(isDark)
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = subject.shortLabel,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatusChip(status: ChapterStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        ChapterStatus.NOT_STARTED -> MaterialTheme.colorScheme.onSurfaceVariant
        ChapterStatus.CURRENTLY_STUDYING -> MaterialTheme.colorScheme.primary
        ChapterStatus.THEORY_COMPLETED -> MaterialTheme.colorScheme.primary
        ChapterStatus.PRACTICE_IN_PROGRESS -> SolarAmber
        ChapterStatus.REVISION_IN_PROGRESS -> MintEmerald
        ChapterStatus.STRONG -> MintEmerald
        ChapterStatus.NEEDS_ATTENTION -> AlertRose
    }
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun SkipWithReasonDialog(
    itemTitle: String,
    onDismiss: () -> Unit,
    onConfirmSkip: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Skip & Defer Revision") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Provide a brief reason for deferring \"$itemTitle\" to tomorrow. Overdue tracking stays preserved.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (e.g., Coaching test prep)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("skip_reason_input"),
                    singleLine = false,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmSkip(reason.trim()) },
                enabled = reason.trim().length >= 3,
                modifier = Modifier.testTag("confirm_skip_button")
            ) {
                Text("Defer to Tomorrow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RescheduleDateDialog(
    itemTitle: String,
    currentDueDate: String,
    onDismiss: () -> Unit,
    onConfirmReschedule: (String, String) -> Unit
) {
    val today = IstDateUtils.todayIst()
    val presets = listOf(
        "Today" to today,
        "Tomorrow" to IstDateUtils.addDays(today, 1),
        "+3 Days" to IstDateUtils.addDays(today, 3),
        "+7 Days" to IstDateUtils.addDays(today, 7)
    )
    var selectedDate by remember { mutableStateOf(IstDateUtils.addDays(today, 1)) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reschedule Revision") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "$itemTitle\nOriginal due: ${IstDateUtils.formatDisplay(currentDueDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (label, iso) ->
                        OutlinedButton(
                            onClick = { selectedDate = iso },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = { selectedDate = it },
                    label = { Text("Target Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmReschedule(selectedDate.trim(), reason.trim()) },
                enabled = selectedDate.matches(Regex("""\d{4}-\d{2}-\d{2}""")),
                modifier = Modifier.testTag("confirm_reschedule_button")
            ) {
                Text("Save Date")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RevisionHistoryDialog(
    title: String,
    sessions: List<RevisionSession>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.History, contentDescription = "Revision history")
                Spacer(modifier = Modifier.width(8.dp))
                Text("History: $title", maxLines = 1)
            }
        },
        text = {
            if (sessions.isEmpty()) {
                Text(
                    text = "No completed revision sessions recorded for this item yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sessions, key = { it.id }) { s ->
                        val outcomeColor = when (s.outcomeEnum) {
                            RevisionOutcome.EASY, RevisionOutcome.GOOD -> MintEmerald
                            RevisionOutcome.DIFFICULT -> SolarAmber
                            RevisionOutcome.FORGOT -> AlertRose
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = IstDateUtils.formatDisplay(s.sessionDate),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = "${s.outcomeEnum.label} • ${s.previousIntervalDays}d → ${s.newIntervalDays}d",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = outcomeColor
                                    )
                                }
                                if (s.topicName.isNotBlank()) {
                                    Text(
                                        text = "Topic: ${s.topicName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "Questions: ${s.questionsCorrect}/${s.questionsAttempted} • Understanding ${s.understandingRating}/5 • Confidence ${s.confidenceRating}/5",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (s.rescheduleExplanation.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = s.rescheduleExplanation,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
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
