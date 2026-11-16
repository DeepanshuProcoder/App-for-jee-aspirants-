package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.engine.IstDateUtils
import com.example.domain.model.ChapterStatus
import com.example.domain.model.QueueCategory
import com.example.domain.model.RevisionQueueItem
import com.example.domain.model.Subject
import com.example.ui.components.RescheduleDateDialog
import com.example.ui.components.RevisionHistoryDialog
import com.example.ui.components.SkipWithReasonDialog
import com.example.ui.components.StatusChip
import com.example.ui.components.SubjectBadge
import com.example.ui.theme.AlertRose
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.SolarAmber
import com.example.ui.viewmodel.JeeAppUiState
import kotlin.math.roundToInt

private enum class UrgencyBucketFilter(val label: String) {
    ALL("All"),
    OVERDUE("Overdue"),
    TODAY("Due Today"),
    PRIORITY("Weak & Prep")
}

@Composable
fun TodaysRevisionScreen(
    uiState: JeeAppUiState,
    onStartRevision: (RevisionQueueItem) -> Unit,
    onOpenChapter: (String) -> Unit,
    onReschedule: (RevisionQueueItem, String, String) -> Unit,
    onSkipWithReason: (RevisionQueueItem, String) -> Unit,
    onOpenSubjects: () -> Unit,
    onStartPracticeTest: ((RevisionQueueItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var subjectFilter by remember { mutableStateOf<Subject?>(null) }
    var bucketFilter by remember { mutableStateOf(UrgencyBucketFilter.ALL) }
    var weakOnly by remember { mutableStateOf(false) }
    var includeUpcoming by remember { mutableStateOf(false) }

    var skipTarget by remember { mutableStateOf<RevisionQueueItem?>(null) }
    var rescheduleTarget by remember { mutableStateOf<RevisionQueueItem?>(null) }
    var historyTarget by remember { mutableStateOf<RevisionQueueItem?>(null) }

    val subjectFiltered = remember(uiState.revisionQueue, subjectFilter) {
        if (subjectFilter == null) uiState.revisionQueue
        else uiState.revisionQueue.filter { it.subject == subjectFilter }
    }

    val overdueAll = remember(subjectFiltered) {
        subjectFiltered.filter { it.category == QueueCategory.OVERDUE }
    }
    val todayAll = remember(subjectFiltered) {
        subjectFiltered.filter { it.category == QueueCategory.DUE_TODAY }
    }
    val priorityAll = remember(subjectFiltered) {
        subjectFiltered.filter {
            it.category == QueueCategory.URGENT_WEAK || it.category == QueueCategory.TEST_PREP
        }
    }
    val upcomingAll = remember(subjectFiltered) {
        subjectFiltered.filter { it.category == QueueCategory.UPCOMING }
    }

    val actionableAll = remember(overdueAll, todayAll, priorityAll) {
        (overdueAll + todayAll + priorityAll).sortedByDescending { it.priorityScore }
    }
    val topPriorityItem = actionableAll.firstOrNull()
    val actionableMinutes = remember(actionableAll) { actionableAll.sumOf { it.estimatedMinutes } }
    val completedToday = uiState.dashboardMetrics?.completedTodayCount ?: 0
    val dailyCapacity = (uiState.userProfile?.dailyRevisionCapacity?.toInt() ?: 6).coerceAtLeast(1)

    val filteredQueue = remember(
        subjectFiltered,
        bucketFilter,
        weakOnly,
        includeUpcoming
    ) {
        subjectFiltered.filter { item ->
            val matchesBucket = when (bucketFilter) {
                UrgencyBucketFilter.ALL -> includeUpcoming || item.category != QueueCategory.UPCOMING
                UrgencyBucketFilter.OVERDUE -> item.category == QueueCategory.OVERDUE
                UrgencyBucketFilter.TODAY -> item.category == QueueCategory.DUE_TODAY
                UrgencyBucketFilter.PRIORITY ->
                    item.category == QueueCategory.URGENT_WEAK || item.category == QueueCategory.TEST_PREP
            }
            val matchesWeak = !weakOnly ||
                item.status == ChapterStatus.NEEDS_ATTENTION ||
                item.category == QueueCategory.URGENT_WEAK ||
                item.estimatedRetention < 60.0
            matchesBucket && matchesWeak
        }
    }

    val overdueSection = remember(filteredQueue) {
        filteredQueue.filter { it.category == QueueCategory.OVERDUE }
    }
    val todaySection = remember(filteredQueue) {
        filteredQueue.filter { it.category == QueueCategory.DUE_TODAY }
    }
    val prioritySection = remember(filteredQueue) {
        filteredQueue.filter {
            it.category == QueueCategory.URGENT_WEAK || it.category == QueueCategory.TEST_PREP
        }
    }
    val upcomingSection = remember(filteredQueue) {
        filteredQueue.filter { it.category == QueueCategory.UPCOMING }
    }

    val hasAnyActiveChapters = remember(uiState.chapters) {
        uiState.chapters.any { !it.isArchived && it.statusEnum != ChapterStatus.NOT_STARTED }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Mobile-First Hero Focus Card
        item(key = "today_hero_summary") {
            TodayFocusHeroCard(
                todayDisplay = IstDateUtils.formatDisplay(IstDateUtils.todayIst()),
                overdueCount = overdueAll.size,
                dueTodayCount = todayAll.size,
                priorityCount = priorityAll.size,
                completedToday = completedToday,
                actionableMinutes = actionableMinutes,
                dailyCapacity = dailyCapacity,
                activeBucket = bucketFilter,
                topPriorityItem = topPriorityItem,
                onSelectBucket = { clicked ->
                    bucketFilter = if (bucketFilter == clicked) UrgencyBucketFilter.ALL else clicked
                },
                onStartTopPriority = {
                    if (topPriorityItem != null) onStartRevision(topPriorityItem)
                }
            )
        }

        // 2. Thumb-friendly Horizontal Filter Bar
        item(key = "today_filter_bar") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = subjectFilter == null,
                        onClick = { subjectFilter = null },
                        label = { Text("All Subjects") },
                        modifier = Modifier.testTag("filter_subject_all")
                    )
                }
                items(Subject.entries) { subj ->
                    FilterChip(
                        selected = subjectFilter == subj,
                        onClick = { subjectFilter = if (subjectFilter == subj) null else subj },
                        label = { Text(subj.shortLabel) },
                        modifier = Modifier.testTag("filter_subject_${subj.name.lowercase()}")
                    )
                }
                item {
                    FilterChip(
                        selected = weakOnly,
                        onClick = { weakOnly = !weakOnly },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.WarningAmber,
                                contentDescription = "Filter weak items",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Weak Focus") },
                        modifier = Modifier.testTag("filter_weak_only")
                    )
                }
                item {
                    FilterChip(
                        selected = includeUpcoming,
                        onClick = {
                            includeUpcoming = !includeUpcoming
                            if (includeUpcoming && bucketFilter != UrgencyBucketFilter.ALL) {
                                bucketFilter = UrgencyBucketFilter.ALL
                            }
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Schedule,
                                contentDescription = "Toggle upcoming preview",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("3-Day Preview (${upcomingAll.size})") },
                        modifier = Modifier.testTag("filter_upcoming_preview")
                    )
                }
            }
        }

        // 3. Empty State or Categorized Urgency Sections
        if (filteredQueue.isEmpty()) {
            item(key = "today_empty_state") {
                EmptyQueueStateCard(
                    hasAnyActiveChapters = hasAnyActiveChapters,
                    hasActiveFilters = subjectFilter != null || weakOnly || bucketFilter != UrgencyBucketFilter.ALL,
                    upcomingCount = upcomingAll.size,
                    onResetFilters = {
                        subjectFilter = null
                        bucketFilter = UrgencyBucketFilter.ALL
                        weakOnly = false
                    },
                    onShowUpcoming = {
                        bucketFilter = UrgencyBucketFilter.ALL
                        includeUpcoming = true
                    },
                    onOpenSubjects = onOpenSubjects
                )
            }
        } else {
            if (overdueSection.isNotEmpty()) {
                item(key = "header_overdue") {
                    QueueSectionHeader(
                        title = "Overdue Catch-Up",
                        subtitle = "Past due date • highest retention decay risk",
                        count = overdueSection.size,
                        accent = AlertRose
                    )
                }
                items(overdueSection, key = { it.key }) { item ->
                    RevisionTaskCard(
                        item = item,
                        onStartRevision = { onStartRevision(item) },
                        onOpenChapter = { onOpenChapter(item.chapterId) },
                        onReschedule = { rescheduleTarget = item },
                        onSkip = { skipTarget = item },
                        onHistory = { historyTarget = item },
                        onPracticeTest = onStartPracticeTest?.let { cb -> { cb(item) } }
                    )
                }
            }

            if (todaySection.isNotEmpty()) {
                item(key = "header_due_today") {
                    QueueSectionHeader(
                        title = "Due Today",
                        subtitle = "Scheduled active recall for today",
                        count = todaySection.size,
                        accent = MaterialTheme.colorScheme.primary
                    )
                }
                items(todaySection, key = { it.key }) { item ->
                    RevisionTaskCard(
                        item = item,
                        onStartRevision = { onStartRevision(item) },
                        onOpenChapter = { onOpenChapter(item.chapterId) },
                        onReschedule = { rescheduleTarget = item },
                        onSkip = { skipTarget = item },
                        onHistory = { historyTarget = item },
                        onPracticeTest = onStartPracticeTest?.let { cb -> { cb(item) } }
                    )
                }
            }

            if (prioritySection.isNotEmpty()) {
                item(key = "header_priority") {
                    QueueSectionHeader(
                        title = "Priority & Weak Topics",
                        subtitle = "Low-retention alerts & upcoming test targets",
                        count = prioritySection.size,
                        accent = SolarAmber
                    )
                }
                items(prioritySection, key = { it.key }) { item ->
                    RevisionTaskCard(
                        item = item,
                        onStartRevision = { onStartRevision(item) },
                        onOpenChapter = { onOpenChapter(item.chapterId) },
                        onReschedule = { rescheduleTarget = item },
                        onSkip = { skipTarget = item },
                        onHistory = { historyTarget = item },
                        onPracticeTest = onStartPracticeTest?.let { cb -> { cb(item) } }
                    )
                }
            }

            if (upcomingSection.isNotEmpty()) {
                item(key = "header_upcoming") {
                    QueueSectionHeader(
                        title = "Upcoming Preview (Next 3 Days)",
                        subtitle = "Get ahead early between classes",
                        count = upcomingSection.size,
                        accent = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(upcomingSection, key = { it.key }) { item ->
                    RevisionTaskCard(
                        item = item,
                        onStartRevision = { onStartRevision(item) },
                        onOpenChapter = { onOpenChapter(item.chapterId) },
                        onReschedule = { rescheduleTarget = item },
                        onSkip = { skipTarget = item },
                        onHistory = { historyTarget = item },
                        onPracticeTest = onStartPracticeTest?.let { cb -> { cb(item) } }
                    )
                }
            }
        }
    }

    skipTarget?.let { target ->
        SkipWithReasonDialog(
            itemTitle = target.topicName ?: target.chapterName,
            onDismiss = { skipTarget = null },
            onConfirmSkip = { reason ->
                onSkipWithReason(target, reason)
                skipTarget = null
            }
        )
    }

    rescheduleTarget?.let { target ->
        RescheduleDateDialog(
            itemTitle = target.topicName ?: target.chapterName,
            currentDueDate = target.originalDueDate.ifBlank { target.dueDate },
            onDismiss = { rescheduleTarget = null },
            onConfirmReschedule = { date, reason ->
                onReschedule(target, date, reason)
                rescheduleTarget = null
            }
        )
    }

    historyTarget?.let { target ->
        val relevant = uiState.sessions.filter {
            it.chapterId == target.chapterId && (target.topicId == null || it.topicId == target.topicId)
        }
        RevisionHistoryDialog(
            title = target.topicName ?: target.chapterName,
            sessions = relevant,
            onDismiss = { historyTarget = null }
        )
    }
}

@Composable
private fun TodayFocusHeroCard(
    todayDisplay: String,
    overdueCount: Int,
    dueTodayCount: Int,
    priorityCount: Int,
    completedToday: Int,
    actionableMinutes: Int,
    dailyCapacity: Int,
    activeBucket: UrgencyBucketFilter,
    topPriorityItem: RevisionQueueItem?,
    onSelectBucket: (UrgencyBucketFilter) -> Unit,
    onStartTopPriority: () -> Unit
) {
    val totalActionable = overdueCount + dueTodayCount + priorityCount
    val totalSessionsForDay = (completedToday + totalActionable).coerceAtLeast(1)
    val dayCompletionProgress = (completedToday.toFloat() / totalSessionsForDay.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "IST • $todayDisplay",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Today's Revision",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (totalActionable == 0) {
                                "Queue cleared • $completedToday completed today"
                            } else {
                                "$totalActionable due • ~${actionableMinutes}m load (cap $dailyCapacity/day)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = if (completedToday > 0) MintEmerald.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (completedToday > 0) Icons.Rounded.CheckCircle else Icons.Rounded.Bolt,
                                contentDescription = "Daily progress",
                                tint = if (completedToday > 0) MintEmerald else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$completedToday done",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (completedToday > 0) MintEmerald else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (totalActionable > 0 || completedToday > 0) {
                    LinearProgressIndicator(
                        progress = { dayCompletionProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MintEmerald,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // 3-Pill Interactive Urgency Breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UrgencyMetricPill(
                        label = "Overdue",
                        count = overdueCount,
                        accent = AlertRose,
                        selected = activeBucket == UrgencyBucketFilter.OVERDUE,
                        onClick = { onSelectBucket(UrgencyBucketFilter.OVERDUE) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("urgency_pill_overdue")
                    )
                    UrgencyMetricPill(
                        label = "Today",
                        count = dueTodayCount,
                        accent = MaterialTheme.colorScheme.primary,
                        selected = activeBucket == UrgencyBucketFilter.TODAY,
                        onClick = { onSelectBucket(UrgencyBucketFilter.TODAY) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("urgency_pill_today")
                    )
                    UrgencyMetricPill(
                        label = "Priority",
                        count = priorityCount,
                        accent = SolarAmber,
                        selected = activeBucket == UrgencyBucketFilter.PRIORITY,
                        onClick = { onSelectBucket(UrgencyBucketFilter.PRIORITY) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("urgency_pill_priority")
                    )
                }

                // Quick-Start Highest Priority Action (ideal between coaching classes)
                if (topPriorityItem != null) {
                    val topLabel = topPriorityItem.topicName ?: topPriorityItem.chapterName
                    Button(
                        onClick = onStartTopPriority,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_top_priority_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = "Start top priority revision",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Start Top Priority: $topLabel",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UrgencyMetricPill(
    label: String,
    count: Int,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (count > 0 || selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QueueSectionHeader(
    title: String,
    subtitle: String,
    count: Int,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Surface(
            color = accent.copy(alpha = 0.14f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RevisionTaskCard(
    item: RevisionQueueItem,
    onStartRevision: () -> Unit,
    onOpenChapter: () -> Unit,
    onReschedule: () -> Unit,
    onSkip: () -> Unit,
    onHistory: () -> Unit,
    onPracticeTest: (() -> Unit)?
) {
    var showOverflow by remember { mutableStateOf(false) }

    val urgencyColor = when (item.category) {
        QueueCategory.OVERDUE -> AlertRose
        QueueCategory.DUE_TODAY -> MaterialTheme.colorScheme.primary
        QueueCategory.URGENT_WEAK -> SolarAmber
        QueueCategory.TEST_PREP -> MintEmerald
        QueueCategory.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val retentionPct = item.estimatedRetention.roundToInt().coerceIn(0, 100)
    val retentionColor = when {
        retentionPct < 45 -> AlertRose
        retentionPct < 70 -> SolarAmber
        else -> MintEmerald
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("queue_card_${item.key}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.category == QueueCategory.OVERDUE || item.status == ChapterStatus.NEEDS_ATTENTION) {
                urgencyColor.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left urgency rail
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(urgencyColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metadata badges row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        SubjectBadge(item.subject)
                        Surface(
                            color = urgencyColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = when {
                                    item.category == QueueCategory.OVERDUE && item.overdueDays > 0 ->
                                        "${item.overdueDays}d Overdue"
                                    else -> item.category.label
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = urgencyColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    StatusChip(item.status)
                }

                // Clear Topic-over-Chapter Title Hierarchy
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = item.topicName ?: item.chapterName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (item.topicName != null) {
                        Text(
                            text = "Chapter: ${item.chapterName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Retention Weakness Micro-Meter + Reason & Effort
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.subtitleReason} • ~${item.estimatedMinutes} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$retentionPct% ret",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = retentionColor
                        )
                    }
                    LinearProgressIndicator(
                        progress = { retentionPct / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = retentionColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                if (item.originalDueDate.isNotBlank() && item.originalDueDate != item.dueDate) {
                    Text(
                        text = "Original due: ${IstDateUtils.formatShort(item.originalDueDate)} → ${IstDateUtils.formatShort(item.dueDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SolarAmber
                    )
                }

                // Clean Mobile-First Action Bar (48dp touch targets)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onStartRevision,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("start_revision_${item.key}"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = "Start revision",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Revise")
                    }

                    IconButton(
                        onClick = onReschedule,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CalendarToday,
                            contentDescription = "Reschedule revision"
                        )
                    }

                    IconButton(
                        onClick = onSkip,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            contentDescription = "Skip with reason"
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showOverflow = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = "More revision actions"
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflow,
                            onDismissRequest = { showOverflow = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open Chapter") },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = "Open chapter")
                                },
                                onClick = {
                                    showOverflow = false
                                    onOpenChapter()
                                }
                            )
                            if (onPracticeTest != null) {
                                DropdownMenuItem(
                                    text = { Text("Take AI Practice Test") },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.AutoAwesome, contentDescription = "Take practice test")
                                    },
                                    onClick = {
                                        showOverflow = false
                                        onPracticeTest()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Revision History") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.History, contentDescription = "View revision history")
                                },
                                onClick = {
                                    showOverflow = false
                                    onHistory()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyQueueStateCard(
    hasAnyActiveChapters: Boolean,
    hasActiveFilters: Boolean,
    upcomingCount: Int,
    onResetFilters: () -> Unit,
    onShowUpcoming: () -> Unit,
    onOpenSubjects: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_queue),
                contentDescription = "Empty revision queue illustration",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = when {
                    hasActiveFilters -> "No tasks match your filters"
                    hasAnyActiveChapters -> "All caught up for today!"
                    else -> "No revisions scheduled yet"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when {
                    hasActiveFilters ->
                        "Clear subject or urgency filters to inspect the rest of your revision queue."
                    hasAnyActiveChapters ->
                        "Great discipline! All overdue, due-today, and weak-topic revisions are complete."
                    else ->
                        "Open Subjects to mark Class 11 chapters or topics you are studying and launch your spaced-repetition cycle."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when {
                hasActiveFilters -> {
                    Button(
                        onClick = onResetFilters,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Show All Due Tasks")
                    }
                }
                hasAnyActiveChapters && upcomingCount > 0 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onShowUpcoming,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Preview Next 3 Days ($upcomingCount)")
                        }
                        Button(
                            onClick = onOpenSubjects,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Syllabus")
                        }
                    }
                }
                else -> {
                    Button(
                        onClick = onOpenSubjects,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Browse Syllabus")
                    }
                }
            }
        }
    }
}
