package com.stevepopovich.posture_reminder

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.util.concurrent.TimeUnit

const val CHANNEL_ID = "REMINDER_CHANNEL"
const val NOTIFICATION_ID = 1911
const val REMINDER_WORKER_TAG = "REMINDER_WORKER_TAG"

class ReminderWorker(appContext: Context, workerParams: WorkerParameters): CoroutineWorker(appContext, workerParams) {
    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        //Build push notification
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(androidx.core.R.drawable.ic_call_answer_low)
            .setContentText("Time to check your posture")

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, builder.build())

        val reminderDeleterWorkRequest: WorkRequest =
            OneTimeWorkRequestBuilder<ReminderDeleterWorker>()
                .addTag("reminder_deleter")
                .setInitialDelay(3, TimeUnit.SECONDS)
                .build()

        applicationContext.reminderDataStore.data
            .map { preferences ->
                val minutes = preferences[MINUTES_KEY] ?: 0
                val seconds = preferences[SECONDS_KEY] ?: 0

                val totalSeconds = (minutes * 60) + seconds

                val nextReminderWorkRequest: WorkRequest =
                    OneTimeWorkRequestBuilder<ReminderWorker>()
                        .addTag(REMINDER_WORKER_TAG)
                        .setInitialDelay(totalSeconds.toLong(), TimeUnit.SECONDS)
                        .build()

                val workManager = WorkManager.getInstance(applicationContext)
                workManager.enqueue(reminderDeleterWorkRequest)
                workManager.enqueue(nextReminderWorkRequest)
            }.collect()

        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}