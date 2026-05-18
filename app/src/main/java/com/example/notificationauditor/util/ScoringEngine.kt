package com.example.notificationauditor.util

import android.service.notification.NotificationListenerService
import com.example.notificationauditor.data.db.entity.NotificationEvent
import kotlin.math.max

data class ChannelScore(
    val packageName: String,
    val channelId: String,
    val utilityScore: Float,
    val totalPosted: Int,
    val clicks: Int,
    val dismissals: Int,
    val ghostOpens: Int,
    val shouldRecommendDisable: Boolean
)

object ScoringEngine {

    // Channels scoring below this threshold are flagged for disabling
    const val DISABLE_THRESHOLD = 0.20f

    // Mass-clear dismissals count at this fraction of a deliberate swipe
    private const val MASS_CLEAR_WEIGHT = 0.1f

    /**
     * Computes a utility score in range [0, 1] for a notification channel.
     *
     * Score = (clicks + ghostOpens) / totalPosted
     *         - (weightedDismissals * timeToActFactor)
     *
     * timeToActFactor scales [0, 1] inversely with median time-to-dismiss:
     * fast deliberate swipes (< 5s) push the factor toward 1.0, indicating
     * the user consciously rejected this notification.
     */
    fun score(events: List<NotificationEvent>): ChannelScore? {
        if (events.isEmpty()) return null

        val pkg = events.first().packageName
        val channel = events.first().channelId

        val total = events.size
        val clicks = events.count {
            it.removalReason == NotificationListenerService.REASON_CLICK
        }
        val ghostOpens = events.count { it.isGhostOpen }
        val massClearDismissals = events.count { it.isMassClear }
        val deliberateDismissals = events.count {
            !it.isMassClear &&
            it.removalReason == NotificationListenerService.REASON_CANCEL
        }

        val weightedDismissals = deliberateDismissals + (massClearDismissals * MASS_CLEAR_WEIGHT)

        val medianLatencyMs = medianDismissLatency(events.filter {
            !it.isMassClear && it.removalReason == NotificationListenerService.REASON_CANCEL
        })

        // Fast swipes (< 5 s) → factor near 1.0 (user clearly didn't want it)
        // Slow swipes (> 60 s) → factor near 0.1 (lower penalty, could be habit)
        val timeToActFactor = when {
            medianLatencyMs == null -> 0.5f
            medianLatencyMs < 5_000 -> 1.0f
            medianLatencyMs < 15_000 -> 0.75f
            medianLatencyMs < 60_000 -> 0.5f
            else -> 0.1f
        }

        val positiveSignal = (clicks + ghostOpens).toFloat() / max(total, 1)
        val negativePenalty = (weightedDismissals * timeToActFactor) / max(total, 1)
        val utilityScore = (positiveSignal - negativePenalty).coerceIn(0f, 1f)

        return ChannelScore(
            packageName = pkg,
            channelId = channel,
            utilityScore = utilityScore,
            totalPosted = total,
            clicks = clicks,
            dismissals = deliberateDismissals + massClearDismissals,
            ghostOpens = ghostOpens,
            shouldRecommendDisable = utilityScore < DISABLE_THRESHOLD && total >= 5
        )
    }

    private fun medianDismissLatency(dismissEvents: List<NotificationEvent>): Long? {
        val latencies = dismissEvents.mapNotNull { event ->
            val removeTs = event.removeTimestamp ?: return@mapNotNull null
            // Normalize DND: if intercepted by DND, we can't compute a meaningful latency here
            // (the AnalyticsWorker corrects timestamps before calling score())
            (removeTs - event.postTimestamp).takeIf { it >= 0 }
        }.sorted()

        if (latencies.isEmpty()) return null
        val mid = latencies.size / 2
        return if (latencies.size % 2 == 0) (latencies[mid - 1] + latencies[mid]) / 2
        else latencies[mid]
    }
}
