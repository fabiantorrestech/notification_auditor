package com.example.notificationauditor.ui.insights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.notificationauditor.data.db.AppDatabase
import com.example.notificationauditor.data.repository.NotificationRepository
import kotlinx.coroutines.launch
import org.json.JSONArray

data class ChannelBreakdown(
    val packageName: String,
    val channelId: String,
    val utilityScore: Float,
    val totalPosted: Int,
    val clicks: Int,
    val dismissals: Int,
    val ghostOpens: Int,
    val flagged: Boolean
)

class InsightDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val date: String = checkNotNull(savedStateHandle["date"])
    private val repository = NotificationRepository(AppDatabase.getInstance(application))

    private val _summary = MutableLiveData<String>()
    val summary: LiveData<String> = _summary

    private val _breakdown = MutableLiveData<List<ChannelBreakdown>>()
    val breakdown: LiveData<List<ChannelBreakdown>> = _breakdown

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val insight = repository.getInsightByDate(date) ?: return@launch
            _summary.value = buildString {
                append(insight.date)
                append("  ·  ")
                append("${insight.totalPosted} posted")
                append("  ${insight.clicks} clicks")
                append("  ${insight.dismissals} dismissed")
                if (insight.ghostOpens > 0) append("  ${insight.ghostOpens} ghost opens")
            }
            _breakdown.value = parseBreakdown(insight.channelBreakdownJson)
        }
    }

    private fun parseBreakdown(json: String): List<ChannelBreakdown> {
        val result = mutableListOf<ChannelBreakdown>()
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return result
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                ChannelBreakdown(
                    packageName = obj.getString("packageName"),
                    channelId = obj.getString("channelId"),
                    utilityScore = obj.getDouble("utilityScore").toFloat(),
                    totalPosted = obj.getInt("totalPosted"),
                    clicks = obj.getInt("clicks"),
                    dismissals = obj.getInt("dismissals"),
                    ghostOpens = obj.optInt("ghostOpens", 0),
                    flagged = obj.optBoolean("flagged", false)
                )
            )
        }
        return result
    }
}
