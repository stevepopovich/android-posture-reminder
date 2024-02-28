package com.stevepopovich.posture_reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

const val CHANNEL_ID = "REMINDER_CHANNEL"
const val NOTIFICATION_ID = 1911
const val REMINDER_WORKER_TAG = "REMINDER_WORKER_TAG"

class ReminderWorker(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {
    @SuppressLint("MissingPermission")
    override fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Posture reminder push notifications"
            val descriptionText = "Posture Reminder push notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system.
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        //Build push notification
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(androidx.core.R.drawable.ic_call_answer_low)
            .setContentText("Time to check your posture")
            .setPriority(NotificationCompat.PRIORITY_MAX)

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, builder.build())

        val reminderDeleterWorkRequest: WorkRequest =
            OneTimeWorkRequestBuilder<ReminderDeleterWorker>()
                .addTag("reminder_deleter")
                .setInitialDelay(3, TimeUnit.SECONDS)
                .build()

        val nextReminderWorkRequest: WorkRequest =
            OneTimeWorkRequestBuilder<ReminderWorker>()
                .addTag(REMINDER_WORKER_TAG)
                .setInitialDelay(5, TimeUnit.MINUTES)
                .build()

        val workManager = WorkManager.getInstance(applicationContext)

        workManager.enqueue(reminderDeleterWorkRequest)
        workManager.enqueue(nextReminderWorkRequest)

        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}