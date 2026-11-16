package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.engine.IstDateUtils
import com.example.domain.model.AlertSeverity
import com.example.domain.model.RevisionQueueItem
import com.example.ui.components.MetricStatCard
import com.example.ui.components.RetentionLineChart
import com.example.ui.theme.AlertRose
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.SolarAmber
import com.example.ui.viewmodel.JeeAppUiState
import kotlin.math.roundToInt

@Composable
fun HomeDashboardScreen(
    uiState: JeeAppUiState,
    onStartTodaysRevision: () -> Unit,
    onStartQueueItem: (RevisionQueueItem) -> Unit,
    onOpenChapter: (String) -> Unit,
    onOpenSubjects: () -> Unit,
    onOpenAnalytics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = uiState.dashboardMetrics
    val isDark = isSystemInDarkTheme()
    val todayDisplay = IstDateUtils.formatDisplay(IstDateUtils.todayIst())

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero CTA Card: START TODAY'S REVISION
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "IST • $todayDisplay",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Welcome back, ${uiState.userProfile?.displayName ?: "Aspirant"}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.LocalFireDepartment,
                                        contentDescription = "Revision streak",
                                        tint = SolarAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${metrics?.revisionStreakDays ?: 0}d streak",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }

                        val remaining = metrics?.remainingTodayCount ?: 0
                        val estMin = metrics?.estimatedStudyMinutes ?: 0
                        Text(
                            text = if (remaining > 0) {
                                "$remaining revision${if (remaining == 1) "" else "s"} queued (~$estMin min total). Focus on overdue & weak concepts first."
                            } else {
                                "Your queue is clear right now. Mark a Class 11 chapter as studying or log a quick topic review."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                val firstActive = uiState.revisionQueue.firstOrNull {
                                    it.overdueDays > 0 || it.dueDate == IstDateUtils.todayIst()
                                } ?: uiState.revisionQueue.firstOrNull()
                                if (firstActive != null && remaining > 0) {
                                    onStartQueueItem(firstActive)
                                } else {
                                    onStartTodaysRevision()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_todays_revision_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Start Today's Revision")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "START TODAY'S REVISION",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Today's Overview Grid (2 rows x 3 cards)
        item {
            Text(
                text = "Today's Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "Due Today",
                        value = "${metrics?.dueTodayCount ?: 0}",
                        subtitle = "Scheduled",
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "Completed",
                        value = "${metrics?.completedTodayCount ?: 0}",
                        subtitle = "Sessions today",
                        accent = MintEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "Overdue",
                        value = "${metrics?.overdueCount ?: 0}",
                        subtitle = "Carry-forward",
                        accent = if ((metrics?.overdueCount ?: 0) > 0) AlertRose else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "Remaining",
                        value = "${metrics?.remainingTodayCount ?: 0}",
                        subtitle = "Active queue",
                        accent = SolarAmber,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "Upcoming 7d",
                        value = "${metrics?.upcoming7DaysCount ?: 0}",
                        subtitle = "Next week",
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "Est. Time",
                        value = "${metrics?.estimatedStudyMinutes ?: 0}m",
                        subtitle = "Queue load",
                        accent = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Subject-Wise Overview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subject Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = onOpenSubjects,
                    modifier = Modifier.testTag("dashboard_manage_syllabus_button")
                ) {
                    Icon(Icons.Rounded.MenuBook, contentDescription = "Open syllabus", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Syllabus")
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                (metrics?.subjectSummaries ?: emptyList()).forEach { summary ->
                    val color = summary.subject.accentColor(isDark)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSubjects() },
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
                                Text(
                                    text = summary.subject.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = color,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${summary.studiedChapters}/${summary.totalChapters} active • ${summary.strongChapters} strong",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val frac = if (summary.totalChapters > 0) {
                                summary.studiedChapters.toFloat() / summary.totalChapters.toFloat()
                            } else 0f
                            LinearProgressIndicator(
                                progress = { frac },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val retLabel = summary.averageEstimatedRetention?.let { "${it.roundToInt()}% est. retention" }
                                    ?: "No revisions logged yet"
                                val accLabel = summary.averageAccuracy?.let { "${it.roundToInt()}% accuracy" }
                                    ?: "No Qs logged"
                                Text(
                                    text = retLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$accLabel • ${summary.dueOrOverdueCount} due",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (summary.needsAttentionChapters > 0) AlertRose else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Performance Indicators & Retention Trend
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Retention & Completion Indicators",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Estimated retention is heuristic based on recall & decay",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = onOpenAnalytics) {
                            Text("Reports")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val wkText = metrics?.weeklyCompletionRate?.let { "${it.roundToInt()}%" } ?: "—"
                        val moText = metrics?.monthlyCompletionRate?.let { "${it.roundToInt()}%" } ?: "—"
                        val retText = metrics?.overallEstimatedRetention?.let { "${it.roundToInt()}%" } ?: "—"
                        MetricStatCard("Weekly Rate", wkText, modifier = Modifier.weight(1f))
                        MetricStatCard("Monthly Rate", moText, modifier = Modifier.weight(1f))
                        MetricStatCard(
                            "Est. Retention",
                            retText,
                            accent = MintEmerald,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Strong chapters: ${metrics?.strongChaptersCount ?: 0}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MintEmerald
                        )
                        Text(
                            text = "Weak / Attention: ${metrics?.weakChaptersCount ?: 0}",
                            style = MaterialTheme.typography.labelMedium,
                            color = AlertRose
                        )
                    }

                    RetentionLineChart(points = metrics?.retentionTrendPoints ?: emptyList())
                }
            }
        }

        // Important Alerts
        item {
            Text(
                text = "Important Alerts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        val alerts = metrics?.alerts ?: emptyList()
        if (alerts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_queue),
                            contentDescription = "All alerts clear illustration",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "No urgent alerts",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Start studying chapters in Subjects or add upcoming ALLEN test dates in Calendar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(alerts.size) { index ->
                val alert = alerts[index]
                val accent = when (alert.severity) {
                    AlertSeverity.HIGH -> AlertRose
                    AlertSeverity.MEDIUM -> SolarAmber
                    AlertSeverity.INFO -> MaterialTheme.colorScheme.primary
                }
                val icon = when (alert.severity) {
                    AlertSeverity.HIGH -> Icons.Rounded.ErrorOutline
                    AlertSeverity.MEDIUM -> Icons.Rounded.WarningAmber
                    AlertSeverity.INFO -> Icons.Rounded.Schedule
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (alert.chapterId != null) onOpenChapter(alert.chapterId)
                            else onStartTodaysRevision()
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(icon, contentDescription = alert.title, tint = accent)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(alert.title, style = MaterialTheme.typography.titleSmall, color = accent)
                            Text(
                                alert.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Open alert",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Top Recommendations Preview
        if (uiState.recommendations.isNotEmpty()) {
            item {
                val rec = uiState.recommendations.first()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenChapter(rec.chapterId) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = "Smart recommendation",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rec.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(rec.message, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
