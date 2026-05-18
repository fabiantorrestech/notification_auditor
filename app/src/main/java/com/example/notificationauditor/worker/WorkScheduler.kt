package com.example.notificationauditor.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.notificationauditor.util.DailyCutoffTime
import java.util.concurrent.TimeUnit

object WorkScheduler {

    private const val ANALYTICS_WORK_NAME = "nightly_analytics"

    fun scheduleAnalytics(context: Context) {
        enqueuePeriodicWork(context, ExistingPeriodicWorkPolicy.KEEP)
    }

    fun rescheduleAnalytics(context: Context) {
        enqueuePeriodicWork(context, ExistingPeriodicWorkPolicy.REPLACE)
    }

    private fun enqueuePeriodicWork(
        context: Context,
        policy: ExistingPeriodicWorkPolicy
    ) {
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(true)
            .build()

        val cutoff = DailyCutoffTime.load(context)
        val request = PeriodicWorkRequestBuilder<AnalyticsWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(
                DailyCutoffTime.nextDelayMillis(System.currentTimeMillis(), cutoff),
                TimeUnit.MILLISECONDS
            )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ANALYTICS_WORK_NAME,
            policy,
            request
        )
    }
}
