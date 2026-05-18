package com.example.notificationauditor.ui.dashboard

import android.app.Application
import android.service.notification.NotificationListenerService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.notificationauditor.data.db.AppDatabase
import com.example.notificationauditor.data.db.entity.StudySession
import com.example.notificationauditor.data.repository.NotificationRepository
import com.example.notificationauditor.util.AppPrefs
import com.example.notificationauditor.util.ChannelScore
import com.example.notificationauditor.util.ScoringEngine
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.concurrent.TimeUnit

data class RecommendationItem(
    val packageName: String,
    val channelId: String,
    val utilityScore: Float,
    val totalPosted: Int
)

data class PreviewResult(
    val totalPosted: Int,
    val clicks: Int,
    val dismissals: Int,
    val ghostOpens: Int,
    val recommendations: List<ChannelScore>
)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = NotificationRepository(AppDatabase.getInstance(app))

    val activeSession: LiveData<StudySession?> = repository.observeActiveSession()

    private val _totalLogged = MutableLiveData(0)
    val totalLogged: LiveData<Int> = _totalLogged

    private val _recommendations = MutableLiveData<List<RecommendationItem>>(emptyList())
    val recommendations: LiveData<List<RecommendationItem>> = _recommendations

    private val _previewResult = MutableLiveData<PreviewResult?>()
    val previewResult: LiveData<PreviewResult?> = _previewResult

    val studyDurationDays: LiveData<Long> = activeSession.switchMap { session ->
        val liveData = MutableLiveData<Long>()
        liveData.value = session?.let {
            TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it.startTimestamp)
        } ?: 0L
        liveData
    }

    fun refresh() {
        viewModelScope.launch {
            val session = repository.getActiveSession() ?: return@launch
            _totalLogged.value = repository.countEventsForSession(session.sessionId)

            val latestInsight = repository.getLatestInsight() ?: return@launch
            _recommendations.value = parseRecommendations(latestInsight.recommendationsJson)
        }
    }

    fun startNewStudy() {
        viewModelScope.launch {
            repository.startNewSession()
            refresh()
        }
    }

    fun clearStudyData() {
        viewModelScope.launch {
            val session = repository.getActiveSession() ?: return@launch
            repository.deleteEventsForSession(session.sessionId)
            refresh()
        }
    }

    fun previewAnalysis() {
        viewModelScope.launch {
            val session = repository.getActiveSession() ?: return@launch
            val windowEnd = System.currentTimeMillis()
            val windowStart = windowEnd - TimeUnit.DAYS.toMillis(1)
            val sevenDaysAgo = windowEnd - TimeUnit.DAYS.toMillis(7)

            val events = repository.getEventsInWindow(session.sessionId, windowStart, windowEnd)
            val totalPosted = events.size
            val clicks = events.count {
                it.removalReason == NotificationListenerService.REASON_CLICK
            }
            val dismissals = events.count {
                !it.isMassClear && it.removalReason == NotificationListenerService.REASON_CANCEL
            }
            val ghostOpens = events.count { it.isGhostOpen }

            val threshold = getApplication<Application>()
                .getSharedPreferences(AppPrefs.NAME, android.content.Context.MODE_PRIVATE)
                .getFloat(AppPrefs.KEY_THRESHOLD, AppPrefs.DEFAULT_THRESHOLD)

            val channels = repository.getActiveChannels(session.sessionId, sevenDaysAgo, windowEnd)
            val recommendations = mutableListOf<ChannelScore>()
            for (channel in channels) {
                val channelEvents = repository.getChannelEventsInWindow(
                    session.sessionId, channel.packageName, channel.channelId, sevenDaysAgo, windowEnd
                )
                val score = ScoringEngine.score(channelEvents, threshold) ?: continue
                if (score.shouldRecommendDisable) recommendations.add(score)
            }

            _previewResult.value = PreviewResult(totalPosted, clicks, dismissals, ghostOpens, recommendations)
        }
    }

    fun clearPreviewResult() {
        _previewResult.value = null
    }

    private fun parseRecommendations(json: String): List<RecommendationItem> {
        val result = mutableListOf<RecommendationItem>()
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return result
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                RecommendationItem(
                    packageName = obj.getString("packageName"),
                    channelId = obj.getString("channelId"),
                    utilityScore = obj.getDouble("utilityScore").toFloat(),
                    totalPosted = obj.getInt("totalPosted")
                )
            )
        }
        return result
    }
}
