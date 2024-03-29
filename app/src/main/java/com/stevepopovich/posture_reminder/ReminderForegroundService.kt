package com.stevepopovich.posture_reminder

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Calendar

const val CHANNEL_ID = "posture_reminder_channel"
const val NOTIFICATION_ID = 1911
const val FOREGROUND_NOTIFICATION_ID = 999
const val DESTROYED_NOTIFICATION_ID = 1000

class ReminderForegroundService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    private val notifyIntent
        get() = Intent(this, MainActivity::class.java)
    private val notifyPendingIntent: PendingIntent
        get() = PendingIntent.getActivity(
            this, 0, notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.posture_reminder_push_notifications_name)
            val descriptionText = getString(R.string.posture_reminder_push_notifications_channel_description)
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
            .setContentTitle(getString(R.string.posture_reminder))
            .setContentText(getString(R.string.your_reminder_timer_is_running_in_the_background))
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setOngoing(true)
            .setContentIntent(notifyPendingIntent)
            .build()

        startForeground(FOREGROUND_NOTIFICATION_ID, notification)

        scope.launch {
            applicationContext.reminderDataStore.data
                .map { preferences ->
                    val minutes = preferences[MINUTES_KEY] ?: 0
                    val seconds = preferences[SECONDS_KEY] ?: 0
                    doReminders(minutes, seconds)
                }.collect()
        }

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentText(getString(R.string.your_reminder_timer_has_been_destroyed))
            .setContentIntent(notifyPendingIntent)

        NotificationManagerCompat.from(applicationContext).notify(DESTROYED_NOTIFICATION_ID, builder.build())

        job.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun doReminders(minutes: Int, seconds: Int) {
        val totalSeconds = (minutes * 60) + seconds

        val preferences = applicationContext.reminderDataStore.data.firstOrNull()
        val lastNotifTime = preferences?.get(LAST_NOTIF_TIME) ?: formatter.format(Calendar.getInstance().time)
        val timeParsed = formatter.parse(lastNotifTime)

        val now = Calendar.getInstance().time

        val difference = (now.time - timeParsed.time) / 1000

        scope.launch { // Don't wanna block the thread
            applicationContext.reminderDataStore.edit { it[LAST_NOTIF_TIME] = formatter.format(now) }
        }

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentText(getString(R.string.time_to_check_your_posture) + " $difference")
            .setContentIntent(notifyPendingIntent)

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, builder.build())

        val deleteReminderDelay = 3L
        delay(deleteReminderDelay * 1000)

//        NotificationManagerCompat.from(applicationContext).cancel(NOTIFICATION_ID) Need this long term

        delay((totalSeconds.toLong() - deleteReminderDelay) * 1000)

        doReminders(minutes, seconds)
    }
}