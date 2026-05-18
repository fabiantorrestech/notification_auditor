package com.example.notificationauditor.worker

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.notificationauditor.data.db.AppDatabase
import com.example.notificationauditor.data.db.entity.DailyInsight
import com.example.notificationauditor.data.repository.NotificationRepository
import com.example.notificationauditor.service.NotificationHarvesterService
import com.example.notificationauditor.util.ChannelScore
import com.example.notificationauditor.util.ScoringEngine
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AnalyticsWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val repository = NotificationRepository(db)

        val session = repository.getActiveSession() ?: return Result.success()

        val todayLabel = dateFormat.format(Date())
        val windowEnd = System.currentTimeMillis()
        val windowStart = windowEnd - TimeUnit.DAYS.toMillis(1)

        val events = repository.getEventsInWindow(session.sessionId, windowStart, windowEnd)

        // Aggregate day-level totals
        val totalPosted = events.size
        val clicks = events.count { it.removalReason == NotificationListenerService.REASON_CLICK }
        val dismissals = events.count {
            !it.isMassClear && it.removalReason == NotificationListenerService.REASON_CANCEL
        }
        val ghostOpens = events.count { it.isGhostOpen }
        val massClearDismissals = events.count { it.isMassClear }

        // Score each channel over a 7-day rolling window
        val sevenDaysAgo = windowEnd - TimeUnit.DAYS.toMillis(7)
        val channels = repository.getActiveChannels(session.sessionId, sevenDaysAgo, windowEnd)
        val recommendations = mutableListOf<ChannelScore>()

        for (channel in channels) {
            val channelEvents = repository.getChannelEventsInWindow(
                session.sessionId, channel.packageName, channel.channelId, sevenDaysAgo, windowEnd
            )
            val score = ScoringEngine.score(channelEvents) ?: continue
            if (score.shouldRecommendDisable) recommendations.add(score)
        }

        val recommendationsJson = buildRecommendationsJson(recommendations)

        repository.saveInsight(
            DailyInsight(
                date = todayLabel,
                sessionId = session.sessionId,
                totalPosted = totalPosted,
                clicks = clicks,
                dismissals = dismissals,
                ghostOpens = ghostOpens,
                massClearDismissals = massClearDismissals,
                recommendationsJson = recommendationsJson
            )
        )

        // Prune raw events older than 30 days
        val pruneBeforeMs = windowEnd - TimeUnit.DAYS.toMillis(30)
        repository.pruneRawEvents(pruneBeforeMs)

        // Prune insights older than 1 year
        val oneYearAgo = Calendar.getInstance().apply { add(Calendar.YEAR, -1) }
        repository.pruneOldInsights(dateFormat.format(oneYearAgo.time))

        // Proactive NLS rebind health check
        NotificationListenerService.requestRebind(
            ComponentName(applicationContext, NotificationHarvesterService::class.java)
        )

        return Result.success()
    }

    private fun buildRecommendationsJson(scores: List<ChannelScore>): String {
        val array = JSONArray()
        for (score in scores) {
            array.put(JSONObject().apply {
                put("packageName", score.packageName)
                put("channelId", score.channelId)
                put("utilityScore", score.utilityScore)
                put("totalPosted", score.totalPosted)
                put("clicks", score.clicks)
                put("dismissals", score.dismissals)
            })
        }
        return array.toString()
    }
}
