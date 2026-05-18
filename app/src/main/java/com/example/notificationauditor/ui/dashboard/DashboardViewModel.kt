package com.example.notificationauditor.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.notificationauditor.data.db.AppDatabase
import com.example.notificationauditor.data.db.entity.StudySession
import com.example.notificationauditor.data.repository.NotificationRepository
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.concurrent.TimeUnit

data class RecommendationItem(
    val packageName: String,
    val channelId: String,
    val utilityScore: Float,
    val totalPosted: Int
)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = NotificationRepository(AppDatabase.getInstance(app))

    val activeSession: LiveData<StudySession?> = repository.observeActiveSession()

    private val _totalLogged = MutableLiveData(0)
    val totalLogged: LiveData<Int> = _totalLogged

    private val _recommendations = MutableLiveData<List<RecommendationItem>>(emptyList())
    val recommendations: LiveData<List<RecommendationItem>> = _recommendations

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
