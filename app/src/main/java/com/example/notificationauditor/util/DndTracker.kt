package com.example.notificationauditor.util

import android.app.NotificationManager
import android.content.Context
import android.service.notification.NotificationListenerService

class DndTracker(context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun isDndActive(): Boolean =
        notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

    /**
     * Returns true if this specific notification was suppressed by DND at the time of posting.
     * Must be called from within a NotificationListenerService using the current ranking.
     */
    fun isInterceptedByDnd(
        key: String,
        rankingMap: NotificationListenerService.RankingMap
    ): Boolean {
        val ranking = NotificationListenerService.Ranking()
        if (!rankingMap.getRanking(key, ranking)) return false
        return !ranking.matchesInterruptionFilter()
    }
}
