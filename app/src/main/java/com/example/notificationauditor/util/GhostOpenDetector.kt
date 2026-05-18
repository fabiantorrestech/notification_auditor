package com.example.notificationauditor.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

class GhostOpenDetector(context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // Window (ms) around the dismissal timestamp to look for a foreground event
    private val windowMs = 10_000L

    /**
     * Returns true if [packageName] was brought to the foreground within [windowMs]
     * of [dismissalTimestamp]. Requires PACKAGE_USAGE_STATS permission.
     */
    fun wasOpenedAround(packageName: String, dismissalTimestamp: Long): Boolean {
        val from = dismissalTimestamp - windowMs
        val to = dismissalTimestamp + windowMs

        val events = usageStatsManager.queryEvents(from, to) ?: return false
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName == packageName &&
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                return true
            }
        }
        return false
    }
}
