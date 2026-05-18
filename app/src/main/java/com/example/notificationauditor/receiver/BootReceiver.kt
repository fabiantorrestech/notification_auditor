package com.example.notificationauditor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.notificationauditor.worker.WorkScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            WorkScheduler.scheduleAnalytics(context)
        }
    }
}
