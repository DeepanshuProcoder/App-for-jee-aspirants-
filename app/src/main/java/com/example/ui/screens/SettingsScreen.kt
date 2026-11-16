package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domain.engine.BackupBundle
import com.example.domain.engine.BackupSerializer
import com.example.domain.engine.IstDateUtils
import com.example.domain.model.ClassLevel
import com.example.domain.model.ThemeMode
import com.example.ui.theme.AlertRose
import com.example.ui.viewmodel.JeeAppUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: JeeAppUiState,
    userId: String,
    onUpdateSettings: (Map<String, Any>, String) -> Unit,
    onBuildJsonBackup: () -> String,
    onBuildCsvExport: () -> String,
    onRestoreBackup: (BackupBundle) -> Unit,
    onSignOut: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile = uiState.userProfile

    var classLevel by remember(profile?.classLevel) {
        mutableStateOf(ClassLevel.fromString(profile?.classLevel ?: ClassLevel.CLASS_11.name))
    }
    var syllabusVersion by remember(profile?.syllabusVersion) {
        mutableStateOf(profile?.syllabusVersion ?: "JEE-2026-v1")
    }
    var intervalsText by remember(profile?.defaultIntervals) {
        mutableStateOf((profile?.defaultIntervals ?: listOf(1L, 3L, 7L, 14L, 30L, 60L, 90L)).joinToString(", "))
    }
    var dailyCapacityText by remember(profile?.dailyRevisionCapacity) {
        mutableStateOf((profile?.dailyRevisionCapacity ?: 6L).toString())
    }
    var preferredTime by remember(profile?.preferredStudyTime) {
        mutableStateOf(profile?.preferredStudyTime ?: "18:30")
    }
    var localFeedback by remember { mutableStateOf<String?>(null) }
    var pendingRestoreBundle by remember { mutableStateOf<BackupBundle?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onUpdateSettings(mapOf("notificationsEnabled" to true), "Daily notifications enabled")
        } else {
            localFeedback = "Notification permission denied. Reminders remain off."
            onUpdateSettings(mapOf("notificationsEnabled" to false), "Notification permission denied")
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val payload = onBuildJsonBackup()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(payload.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                localFeedback = "JSON backup exported cleanly."
            }.onFailure {
                localFeedback = "Export failed: ${it.localizedMessage}"
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val csv = onBuildCsvExport()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                localFeedback = "CSV revision report exported."
            }.onFailure {
                localFeedback = "CSV export failed: ${it.localizedMessage}"
            }
        }
    }

    val restoreJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val jsonText = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Unable to read selected file.")
                BackupSerializer.fromJsonString(jsonText, userId)
            }.onSuccess { bundle ->
                pendingRestoreBundle = bundle
            }.onFailure {
                localFeedback = "Invalid backup file: ${it.localizedMessage}"
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings & Backup") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("settings_back_button")
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
            if (localFeedback != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(
                            text = localFeedback!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Account Card
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
                        Text("Account & Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${profile?.displayName ?: "Aspirant"}${profile?.email?.let { " • $it" } ?: ""}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Cloud sync active with offline persistence • Timezone: Asia/Kolkata (IST)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = onSignOut,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRose),
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Sign out")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign Out")
                        }
                    }
                }
            }

            // Study & Spaced Repetition Preferences
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
                        Text("Spaced Repetition & Syllabus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Text("Active Class Level", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ClassLevel.entries.forEach { lvl ->
                                FilterChip(
                                    selected = classLevel == lvl,
                                    onClick = { classLevel = lvl },
                                    label = { Text(lvl.label) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = syllabusVersion,
                            onValueChange = { syllabusVersion = it },
                            label = { Text("Syllabus Version Tag") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = intervalsText,
                            onValueChange = { intervalsText = it },
                            label = { Text("Custom Intervals (days, comma-separated)") },
                            supportingText = { Text("Default: 1, 3, 7, 14, 30, 60, 90") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("intervals_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = dailyCapacityText,
                                onValueChange = { dailyCapacityText = it },
                                label = { Text("Daily Capacity") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = preferredTime,
                                onValueChange = { preferredTime = it },
                                label = { Text("Study Time (IST HH:mm)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val parsedIntervals = intervalsText
                                        .split(",")
                                        .mapNotNull { it.trim().toLongOrNull() }
                                        .filter { it in 1L..365L }
                                        .distinct()
                                        .sorted()
                                        .ifEmpty { listOf(1L, 3L, 7L, 14L, 30L, 60L, 90L) }
                                    val cap = (dailyCapacityText.toLongOrNull() ?: 6L).coerceIn(1L, 30L)
                                    val cleanTime = if (preferredTime.matches(Regex("""\d{2}:\d{2}"""))) preferredTime else "18:30"
                                    onUpdateSettings(
                                        mapOf(
                                            "classLevel" to classLevel.name,
                                            "syllabusVersion" to syllabusVersion.trim().ifBlank { "JEE-2026-v1" },
                                            "defaultIntervals" to parsedIntervals,
                                            "dailyRevisionCapacity" to cap,
                                            "preferredStudyTime" to cleanTime
                                        ),
                                        "Revision preferences saved"
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_settings_button")
                            ) {
                                Icon(Icons.Rounded.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Preferences")
                            }
                            OutlinedButton(
                                onClick = {
                                    intervalsText = "1, 3, 7, 14, 30, 60, 90"
                                }
                            ) {
                                Text("Reset 1..90")
                            }
                        }
                    }
                }
            }

            // Theme Mode
            item {
                val activeTheme = ThemeMode.fromString(profile?.darkModePreference ?: ThemeMode.DARK.name)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = activeTheme == mode,
                                    onClick = {
                                        onUpdateSettings(
                                            mapOf("darkModePreference" to mode.name),
                                            "Theme: ${mode.label}"
                                        )
                                    },
                                    label = { Text(mode.label) },
                                    modifier = Modifier.testTag("theme_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // Notifications
            item {
                val enabled = profile?.notificationsEnabled == true
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.NotificationsActive, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Daily Revision Reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Fires at your IST study time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = enabled,
                                onCheckedChange = { turnOn ->
                                    if (!turnOn) {
                                        onUpdateSettings(mapOf("notificationsEnabled" to false), "Notifications turned off")
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        onUpdateSettings(mapOf("notificationsEnabled" to true), "Daily notifications enabled")
                                    }
                                },
                                modifier = Modifier.testTag("notifications_master_switch")
                            )
                        }

                        if (enabled) {
                            NotificationSubToggle("Today's Revision Queue", profile?.notifyTodayQueue ?: true) {
                                onUpdateSettings(mapOf("notifyTodayQueue" to it), "Notification preferences updated")
                            }
                            NotificationSubToggle("Overdue Carry-Forwards", profile?.notifyOverdue ?: true) {
                                onUpdateSettings(mapOf("notifyOverdue" to it), "Notification preferences updated")
                            }
                            NotificationSubToggle("Upcoming Coaching Tests", profile?.notifyTests ?: true) {
                                onUpdateSettings(mapOf("notifyTests" to it), "Notification preferences updated")
                            }
                            NotificationSubToggle("Weak Topics Alert", profile?.notifyWeakTopics ?: true) {
                                onUpdateSettings(mapOf("notifyWeakTopics" to it), "Notification preferences updated")
                            }
                            NotificationSubToggle("Sunday Weekly Report", profile?.notifyWeeklyReport ?: true) {
                                onUpdateSettings(mapOf("notifyWeeklyReport" to it), "Notification preferences updated")
                            }
                        }
                    }
                }
            }

            // Export & Restore
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
                        Text("Data Portability & Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Export your entire JEE syllabus, revision history, error notebook & test records as JSON or CSV.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { exportJsonLauncher.launch("jee_revision_backup_${IstDateUtils.todayIst()}.json") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_json_button")
                            ) {
                                Icon(Icons.Rounded.Backup, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export JSON")
                            }
                            OutlinedButton(
                                onClick = { exportCsvLauncher.launch("jee_revision_summary_${IstDateUtils.todayIst()}.csv") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_csv_button")
                            ) {
                                Icon(Icons.Rounded.TableChart, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export CSV")
                            }
                        }
                        OutlinedButton(
                            onClick = { restoreJsonLauncher.launch(arrayOf("application/json", "text/plain")) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("restore_json_button")
                        ) {
                            Icon(Icons.Rounded.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore from JSON Backup")
                        }
                    }
                }
            }
        }
    }

    pendingRestoreBundle?.let { bundle ->
        AlertDialog(
            onDismissRequest = { pendingRestoreBundle = null },
            title = { Text("Confirm Backup Restore") },
            text = {
                Text(
                    "Importing this JSON backup (${bundle.exportedAt}) will overwrite matching chapters (${bundle.chapters.size}), topics (${bundle.topics.size}), sessions (${bundle.sessions.size}), test records (${bundle.testRecords.size}), errors (${bundle.errorRecords.size}), and notes (${bundle.formulaNotes.size}) in your cloud workspace. Continue?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreBackup(bundle)
                        pendingRestoreBundle = null
                        localFeedback = "Restoring backup to Firestore..."
                    },
                    modifier = Modifier.testTag("confirm_restore_button")
                ) {
                    Text("Overwrite & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreBundle = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun NotificationSubToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
