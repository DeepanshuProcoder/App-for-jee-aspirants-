package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

class RevisionReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "JEE Revision OS — Daily Check-in"
        val message = intent?.getStringExtra(EXTRA_MESSAGE)
            ?: "Review today's scheduled Physics, Chemistry, and Maths topics."
        showNotification(context, title, message)
    }

    companion object {
        const val CHANNEL_ID = "jee_revision_reminders"
        const val NOTIFICATION_ID = 4102
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Revision & Test Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Alerts for today's revision queue, overdue topics, upcoming tests, and Sunday review."
                }
                manager.createNotificationChannel(channel)
            }
        }

        fun showNotification(context: Context, title: String, message: String) {
            ensureChannel(context)
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
            } catch (_: SecurityException) {
                // Permission not granted; app remains fully functional without notifications
            }
        }
    }
}
