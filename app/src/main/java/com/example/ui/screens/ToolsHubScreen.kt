package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.ReattemptStatus
import com.example.ui.theme.AlertRose
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.SolarAmber
import com.example.ui.viewmodel.JeeAppUiState

@Composable
fun ToolsHubScreen(
    uiState: JeeAppUiState,
    onOpenPracticeTest: () -> Unit,
    onOpenTests: () -> Unit,
    onOpenErrorNotebook: () -> Unit,
    onOpenFormulaBank: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pendingErrors = uiState.errorRecords.count {
        it.statusEnum == ReattemptStatus.PENDING || it.statusEnum == ReattemptStatus.NEEDS_REVIEW
    }
    val latestTest = uiState.testRecords.maxByOrNull { it.testDate }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "JEE Workspace Tools",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Coaching test analysis, mistake notebook, formula library, reports & settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            ToolHubCard(
                title = "AI Chapter & Topic Practice Test",
                subtitle = "Generate custom MCQs, numerical & short-answer tests • auto-reschedules weak topics",
                icon = Icons.Rounded.AutoAwesome,
                accent = MaterialTheme.colorScheme.secondary,
                testTag = "tool_practice_test",
                onClick = onOpenPracticeTest
            )
        }

        item {
            ToolHubCard(
                title = "Test Analysis",
                subtitle = if (latestTest != null) {
                    "${uiState.testRecords.size} logged • Latest: ${latestTest.testName} (${latestTest.totalMarks.toInt()}/${latestTest.maxMarks.toInt()})"
                } else {
                    "Log ALLEN / Mock test scores & auto-flag weak chapters"
                },
                icon = Icons.Rounded.Assessment,
                accent = MaterialTheme.colorScheme.primary,
                testTag = "tool_test_analysis",
                onClick = onOpenTests
            )
        }

        item {
            ToolHubCard(
                title = "Error Notebook",
                subtitle = "$pendingErrors pending reattempt${if (pendingErrors == 1) "" else "s"} • ${uiState.errorRecords.size} total entries",
                icon = Icons.Rounded.ErrorOutline,
                accent = AlertRose,
                testTag = "tool_error_notebook",
                onClick = onOpenErrorNotebook
            )
        }

        item {
            ToolHubCard(
                title = "Formula & Concept Bank",
                subtitle = "${uiState.formulaNotes.size} formulas, reactions, exceptions & photos across P/C/M",
                icon = Icons.Rounded.AutoStories,
                accent = MintEmerald,
                testTag = "tool_formula_bank",
                onClick = onOpenFormulaBank
            )
        }

        item {
            ToolHubCard(
                title = "Analytics & Weekly Reports",
                subtitle = "${uiState.sessions.size} completed sessions • Sunday review & retention trends",
                icon = Icons.Rounded.Insights,
                accent = SolarAmber,
                testTag = "tool_analytics",
                onClick = onOpenAnalytics
            )
        }

        item {
            ToolHubCard(
                title = "Settings, Intervals & Backup",
                subtitle = "Intervals (${uiState.userProfile?.intervalsAsInts()?.joinToString(",") ?: "1,3,7,14,30,60,90"}) • JSON/CSV export",
                icon = Icons.Rounded.Settings,
                accent = MaterialTheme.colorScheme.onSurface,
                testTag = "tool_settings",
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun ToolHubCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = accent.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(icon, contentDescription = title, tint = accent)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Open $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
