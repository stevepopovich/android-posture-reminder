package com.stevepopovich.posture_reminder

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderDeleterWorker(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {
    override fun doWork(): Result {

        NotificationManagerCompat.from(applicationContext).cancel(NOTIFICATION_ID)

        return Result.success()
    }
}