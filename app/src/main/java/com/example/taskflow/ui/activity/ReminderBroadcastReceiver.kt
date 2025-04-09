package com.example.taskflow.ui.activity

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.taskflow.R

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskName = intent.getStringExtra("task_name") ?: return

        // Check if POST_NOTIFICATIONS permission is granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted, proceed with the notification
            sendNotification(context, taskName)
        } else {
            // If permission is not granted, we can request it or log the issue.
            Toast.makeText(context, "Notification permission not granted", Toast.LENGTH_SHORT).show()
            // Optionally request permission from the user if needed (this depends on your app's structure)
        }
    }

    private fun sendNotification(context: Context, taskName: String) {
        try {
            val notificationIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, "taskflow_channel")
                .setSmallIcon(R.drawable.ic_bell)  // Ensure this icon exists in your resources
                .setContentTitle("Task Reminder")
                .setContentText("Don't forget to: $taskName")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(1001, builder.build()) // You can adjust notification ID if needed
        } catch (e: Exception) {
            // Handle the exception, e.g., log it or show a message to the user
            e.printStackTrace()
            // Optionally log the error or show a toast to notify the user
            Toast.makeText(context, "Error sending notification", Toast.LENGTH_SHORT).show()
        }
    }
}
