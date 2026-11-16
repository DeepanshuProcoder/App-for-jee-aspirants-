package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.engine.IstDateUtils
import com.example.domain.model.ChapterStatus
import com.example.domain.model.Subject
import com.example.ui.components.BarDistributionChart
import com.example.ui.components.MetricStatCard
import com.example.ui.components.RetentionLineChart
import com.example.ui.components.SubjectBadge
import com.example.ui.theme.AlertRose
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.SolarAmber
import com.example.ui.viewmodel.JeeAppUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    uiState: JeeAppUiState,
    onOpenChapter: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val metrics = uiState.dashboardMetrics
    val weekly = uiState.weeklyReport
    val monthly = uiState.monthlyReport
    val today = remember { IstDateUtils.todayIst() }

    val workloadBars = remember(uiState.chapters, uiState.topics, today) {
        val activeChapters = uiState.chapters.filter { !it.isArchived }
        val activeTopics = uiState.topics.filter { !it.isArchived }
        (0..6).map { offset ->
            val iso = IstDateUtils.addDays(today, offset)
            val count = activeChapters.count { it.nextRevisionDate == iso } +
                activeTopics.count { it.nextRevisionDate == iso }
            val label = if (offset == 0) "Today" else IstDateUtils.formatShort(iso).take(6)
            val color = if (count > (uiState.userProfile?.dailyRevisionCapacity ?: 6L)) AlertRose
            else if (offset == 0) SolarAmber else MintEmerald
            Triple(label, count.toFloat(), color)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Analytics & Reports") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("analytics_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Retention Trend
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Insights,
                                contentDescription = "Retention trend",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Retention & Mastery Trend",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Heuristic estimates derived strictly from your logged sessions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        RetentionLineChart(points = metrics?.retentionTrendPoints ?: emptyList())
                    }
                }
            }

            // 2. Subject-Wise Breakdown
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
                        Text(
                            text = "Subject Retention & Chapter Strength",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        (metrics?.subjectSummaries ?: emptyList()).forEach { s ->
                            val color = s.subject.accentColor(isDark)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = s.subject.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = color,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${s.strongChapters} strong • ${s.needsAttentionChapters} weak • ${s.studiedChapters}/${s.totalChapters} active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val retFrac = ((s.averageEstimatedRetention ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { retFrac },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = color,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text(
                                    text = "Est. retention: ${s.averageEstimatedRetention?.let { "${it.roundToInt()}%" } ?: "No data"} • Accuracy: ${s.averageAccuracy?.let { "${it.roundToInt()}%" } ?: "No Qs logged"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3. Next 7-Day Workload Distribution
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Next 7-Day Revision Workload (IST)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Daily capacity target: ${uiState.userProfile?.dailyRevisionCapacity ?: 6} items/day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        BarDistributionChart(
                            bars = workloadBars,
                            emptyText = "No chapters or topics scheduled in the next 7 days."
                        )
                    }
                }
            }

            // 4. Weekly Report (Sunday Review)
            if (weekly != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.CalendarViewWeek,
                                        contentDescription = "Weekly report",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Weekly Revision Report",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${IstDateUtils.formatShort(weekly.weekStartDate)} – ${IstDateUtils.formatShort(weekly.weekEndDate)} (Sun Cycle)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (IstDateUtils.isSunday()) {
                                    Surface(
                                        color = MintEmerald.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Sunday Review",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MintEmerald,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricStatCard("Completed", "${weekly.revisionsCompleted}", accent = MintEmerald, modifier = Modifier.weight(1f))
                                MetricStatCard("Missed/Due", "${weekly.revisionsMissed}", accent = AlertRose, modifier = Modifier.weight(1f))
                                MetricStatCard(
                                    "Avg Accuracy",
                                    weekly.averageAccuracy?.let { "${it.roundToInt()}%" } ?: "—",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Text(
                                text = "By Subject: Phy ${weekly.subjectCompletionMap[Subject.PHYSICS] ?: 0} • Chem ${weekly.subjectCompletionMap[Subject.CHEMISTRY] ?: 0} • Math ${weekly.subjectCompletionMap[Subject.MATHEMATICS] ?: 0} | Repeated Errors: ${weekly.repeatedErrorsCount} | Next Week Load: ${weekly.nextWeekWorkloadCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (weekly.strongChapters.isNotEmpty()) {
                                Text(
                                    text = "Strong: ${weekly.strongChapters.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MintEmerald
                                )
                            }
                            if (weekly.weakChapters.isNotEmpty()) {
                                Text(
                                    text = "Needs Attention: ${weekly.weakChapters.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AlertRose
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Suggested Action Plan", style = MaterialTheme.typography.labelLarge)
                                weekly.suggestedActionPlan.forEach { step ->
                                    Text("• $step", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Monthly Report
            if (monthly != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Monthly Summary (${monthly.monthLabel})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricStatCard("Sessions", "${monthly.totalSessions}", modifier = Modifier.weight(1f))
                                MetricStatCard("Questions", "${monthly.totalQuestionsSolved}", modifier = Modifier.weight(1f))
                                MetricStatCard(
                                    "Strong Ch.",
                                    "${monthly.strongChaptersCount}/${monthly.activeChaptersCount}",
                                    accent = MintEmerald,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Text(
                                text = "Top Error Pattern: ${monthly.topMistakeCategory?.label ?: "None logged yet"} • Avg Accuracy: ${monthly.averageAccuracy?.let { "${it.roundToInt()}%" } ?: "—"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 6. Chapter Status Distribution
            item {
                val active = uiState.chapters.filter { !it.isArchived }
                val weakList = active.filter { it.statusEnum == ChapterStatus.NEEDS_ATTENTION }
                if (weakList.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Chapters Needing Immediate Attention (${weakList.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = AlertRose,
                                fontWeight = FontWeight.Bold
                            )
                            weakList.take(6).forEach { ch ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        SubjectBadge(ch.subjectEnum)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(ch.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                    }
                                    OutlinedButton(onClick = { onOpenChapter(ch.id) }) {
                                        Text("Open")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. Data-Driven Recommendations
            item {
                Text(
                    text = "Smart Recommendations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.recommendations.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = "Complete revision sessions or log mock tests to receive data-backed focus recommendations.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(uiState.recommendations, key = { it.id }) { rec ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SubjectBadge(rec.subject)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(rec.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Text(rec.message, style = MaterialTheme.typography.bodySmall)
                            Button(
                                onClick = { onOpenChapter(rec.chapterId) },
                                modifier = Modifier.testTag("rec_action_${rec.id}")
                            ) {
                                Text(rec.actionLabel)
                            }
                        }
                    }
                }
            }
        }
    }
}
