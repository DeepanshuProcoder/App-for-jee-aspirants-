package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.domain.model.CalendarEvent
import com.example.domain.model.CalendarEventType
import com.example.domain.model.Chapter
import com.example.domain.model.Subject
import com.example.ui.components.RescheduleDateDialog
import com.example.ui.components.SubjectBadge
import com.example.ui.theme.AlertRose
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.SolarAmber
import com.example.ui.viewmodel.JeeAppUiState

@Composable
fun CalendarPlannerScreen(
    uiState: JeeAppUiState,
    onToggleBlockDate: (String) -> Unit,
    onAddEvent: (String, CalendarEventType, String, String, String, String, String) -> Unit,
    onToggleEventDone: (CalendarEvent) -> Unit,
    onDeleteEvent: (String) -> Unit,
    onAutoBalance: () -> Unit,
    onRescheduleChapter: (Chapter, String, String) -> Unit,
    onOpenChapter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = IstDateUtils.todayIst()
    val (initYear, initMonth) = remember { IstDateUtils.currentYearMonth(today) }
    var viewYear by remember { mutableIntStateOf(initYear) }
    var viewMonth by remember { mutableIntStateOf(initMonth) }
    var isWeeklyView by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(today) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var rescheduleChapterTarget by remember { mutableStateOf<Chapter?>(null) }

    val isDark = isSystemInDarkTheme()
    val blockedSet = uiState.userProfile?.blockedDates?.toSet() ?: emptySet()
    val activeChapters = remember(uiState.chapters) { uiState.chapters.filter { !it.isArchived } }

    val monthCells = remember(viewYear, viewMonth) {
        IstDateUtils.getMonthGridDates(viewYear, viewMonth)
    }
    val weekDates = remember(selectedDate) {
        IstDateUtils.getWeekDates(selectedDate)
    }

    val selectedChaptersDue = remember(activeChapters, selectedDate) {
        activeChapters.filter { it.nextRevisionDate == selectedDate }
    }
    val selectedEvents = remember(uiState.calendarEvents, selectedDate) {
        uiState.calendarEvents.filter { it.date == selectedDate }
    }
    val selectedCompleted = remember(uiState.sessions, selectedDate) {
        uiState.sessions.filter { it.sessionDate == selectedDate }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddEventDialog = true },
                modifier = Modifier.testTag("add_calendar_event_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add test or target")
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Calendar & Planner", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Capacity: ${uiState.userProfile?.dailyRevisionCapacity ?: 6}/day • IST",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = !isWeeklyView,
                            onClick = { isWeeklyView = false },
                            label = { Text("Month") }
                        )
                        FilterChip(
                            selected = isWeeklyView,
                            onClick = { isWeeklyView = true },
                            label = { Text("Week") }
                        )
                    }
                }
            }

            // Month Header & Auto-balance
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (viewMonth == 0) {
                                viewMonth = 11; viewYear--
                            } else viewMonth--
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
                        }
                        Text(
                            text = IstDateUtils.monthYearLabel(viewYear, viewMonth),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            if (viewMonth == 11) {
                                viewMonth = 0; viewYear++
                            } else viewMonth++
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
                        }
                    }

                    OutlinedButton(
                        onClick = onAutoBalance,
                        modifier = Modifier.testTag("auto_balance_calendar_button")
                    ) {
                        Icon(Icons.Rounded.AutoFixHigh, contentDescription = "Auto balance workload", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Balance")
                    }
                }
            }

            // Calendar Grid (Monthly or Weekly)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                                Text(d, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        val rows = if (isWeeklyView) listOf(weekDates) else monthCells.chunked(7)
                        rows.forEach { week ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                week.forEach { iso ->
                                    if (iso == null) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    } else {
                                        val isSelected = iso == selectedDate
                                        val isToday = iso == today
                                        val isBlocked = blockedSet.contains(iso)
                                        val dayDue = activeChapters.filter { it.nextRevisionDate == iso }
                                        val hasTest = uiState.calendarEvents.any {
                                            it.date == iso && it.eventTypeEnum == CalendarEventType.ALLEN_TEST
                                        }
                                        val isOverdueDay = iso < today && dayDue.isNotEmpty()
                                        val hasCompleted = uiState.sessions.any { it.sessionDate == iso }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    when {
                                                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                                                        isBlocked -> AlertRose.copy(alpha = 0.14f)
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .border(
                                                    width = if (isToday) 1.5.dp else 0.dp,
                                                    color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable { selectedDate = iso },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = IstDateUtils.dayOfMonth(iso).toString(),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = when {
                                                        isBlocked -> AlertRose
                                                        isOverdueDay -> AlertRose
                                                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    dayDue.map { it.subjectEnum }.distinct().take(3).forEach { subj ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(subj.accentColor(isDark))
                                                        )
                                                    }
                                                    if (hasTest) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(AlertRose)
                                                        )
                                                    }
                                                    if (hasCompleted && dayDue.isEmpty()) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(MintEmerald)
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
                }
            }

            // Selected Day Header + Block Day Toggle
            item {
                val isBlocked = blockedSet.contains(selectedDate)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = IstDateUtils.formatDisplay(selectedDate),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${selectedChaptersDue.size} due • ${selectedEvents.size} events • ${selectedCompleted.size} completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = { onToggleBlockDate(selectedDate) }) {
                        Icon(Icons.Rounded.Block, contentDescription = "Toggle unavailable day", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBlocked) "Unblock Day" else "Block Day")
                    }
                }
            }

            // Events on Selected Date
            items(selectedEvents, key = { "ev_${it.id}" }) { ev ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${ev.eventTypeEnum.label} • ${ev.timeSlot}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (ev.eventTypeEnum == CalendarEventType.ALLEN_TEST) AlertRose else MaterialTheme.colorScheme.primary
                            )
                            Text(ev.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (ev.notes.isNotBlank()) {
                                Text(ev.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Row {
                            IconButton(onClick = { onToggleEventDone(ev) }) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = "Mark event complete",
                                    tint = if (ev.isCompleted) MintEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDeleteEvent(ev.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete event")
                            }
                        }
                    }
                }
            }

            // Chapters Due on Selected Date
            items(selectedChaptersDue, key = { "ch_${it.id}" }) { ch ->
                Card(
                    modifier = Modifier.clickable { onOpenChapter(ch.id) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SubjectBadge(ch.subjectEnum)
                            Text(ch.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (ch.originalDueDate.isNotBlank() && ch.originalDueDate != ch.nextRevisionDate) {
                                Text(
                                    text = "Original: ${IstDateUtils.formatShort(ch.originalDueDate)} → ${IstDateUtils.formatShort(ch.nextRevisionDate)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SolarAmber
                                )
                            }
                        }
                        OutlinedButton(onClick = { rescheduleChapterTarget = ch }) {
                            Text("Move")
                        }
                    }
                }
            }
        }
    }

    if (showAddEventDialog) {
        var title by remember { mutableStateOf("") }
        var eventType by remember { mutableStateOf(CalendarEventType.ALLEN_TEST) }
        var subject by remember { mutableStateOf("ALL") }
        var timeSlot by remember { mutableStateOf("09:00") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddEventDialog = false },
            title = { Text("Add Event (${IstDateUtils.formatShort(selectedDate)})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title (e.g., ALLEN Minor Test 5)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CalendarEventType.entries.forEach { t ->
                            FilterChip(
                                selected = eventType == t,
                                onClick = { eventType = t },
                                label = { Text(t.label, maxLines = 1) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ALL", "PHYSICS", "CHEMISTRY", "MATHEMATICS").forEach { s ->
                            FilterChip(
                                selected = subject == s,
                                onClick = { subject = s },
                                label = { Text(if (s == "ALL") "All" else Subject.valueOf(s).shortLabel) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = timeSlot,
                        onValueChange = { timeSlot = it },
                        label = { Text("Time (HH:mm IST)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Syllabus / Notes") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddEvent(title, eventType, subject, selectedDate, timeSlot, "", notes)
                        showAddEventDialog = false
                    },
                    enabled = title.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = { TextButton(onClick = { showAddEventDialog = false }) { Text("Cancel") } }
        )
    }

    rescheduleChapterTarget?.let { ch ->
        RescheduleDateDialog(
            itemTitle = ch.name,
            currentDueDate = ch.originalDueDate.ifBlank { ch.nextRevisionDate },
            onDismiss = { rescheduleChapterTarget = null },
            onConfirmReschedule = { newDate, reason ->
                onRescheduleChapter(ch, newDate, reason)
                rescheduleChapterTarget = null
            }
        )
    }
}
