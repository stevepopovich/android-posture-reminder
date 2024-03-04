package com.stevepopovich.posture_reminder

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ReminderForegroundService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Posture reminder push notifications"
            val descriptionText = "Posture Reminder push notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system.
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Posture Reminder")
            .setContentText("Your reminder timer is running in the background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        startForeground(999, notification)

        scope.launch {
            applicationContext.reminderDataStore.data
                .map { preferences ->
                    doReminders(preferences)
                }.collect()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        stopForeground(true)
    }

    @SuppressLint("MissingPermission")
    private suspend fun doReminders(preferences: Preferences) {
        val minutes = preferences[MINUTES_KEY] ?: 0
        val seconds = preferences[SECONDS_KEY] ?: 0

        val totalSeconds = (minutes * 60) + seconds

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(androidx.core.R.drawable.ic_call_answer_low)
            .setContentText("Time to check your posture")

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, builder.build())

        val deleteReminderDelay = 3L
        delay(deleteReminderDelay * 1000)

        NotificationManagerCompat.from(applicationContext).cancel(NOTIFICATION_ID)

        delay((totalSeconds.toLong() - deleteReminderDelay) * 1000)

        doReminders(preferences)
    }
}