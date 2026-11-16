package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.domain.engine.IstDateUtils
import com.example.domain.model.DashboardMetrics
import com.example.domain.model.UserProfile
import java.util.Calendar

object ReminderScheduler {

    fun syncDailyReminder(context: Context, profile: UserProfile?, metrics: DashboardMetrics?) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        if (profile == null || !profile.notificationsEnabled) {
            cancelDailyReminder(appContext, alarmManager)
            return
        }

        val summaryParts = mutableListOf<String>()
        if (profile.notifyTodayQueue && (metrics?.dueTodayCount ?: 0) > 0) {
            summaryParts.add("${metrics?.dueTodayCount} due today")
        }
        if (profile.notifyOverdue && (metrics?.overdueCount ?: 0) > 0) {
            summaryParts.add("${metrics?.overdueCount} overdue")
        }
        if (profile.notifyWeakTopics && (metrics?.weakChaptersCount ?: 0) > 0) {
            summaryParts.add("${metrics?.weakChaptersCount} weak chapters")
        }
        if (profile.notifyWeeklyReport && IstDateUtils.isSunday()) {
            summaryParts.add("Sunday Weekly Report ready")
        }
        val body = if (summaryParts.isNotEmpty()) {
            "Focus queue: ${summaryParts.joinToString(" • ")}."
        } else {
            "Check your spaced-repetition queue & upcoming JEE targets."
        }

        val (hour, minute) = parseTime(profile.preferredStudyTime)
        val cal = Calendar.getInstance(IstDateUtils.IST_TIMEZONE).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(appContext, RevisionReminderReceiver::class.java).apply {
            putExtra(RevisionReminderReceiver.EXTRA_TITLE, "JEE Revision OS")
            putExtra(RevisionReminderReceiver.EXTRA_MESSAGE, body)
        }
        val pi = PendingIntent.getBroadcast(
            appContext,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pi
        )
    }

    private fun cancelDailyReminder(context: Context, alarmManager: AlarmManager) {
        val intent = Intent(context, RevisionReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    private fun parseTime(hhmm: String): Pair<Int, Int> {
        val parts = hhmm.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 18
        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 30
        return h to m
    }
}
