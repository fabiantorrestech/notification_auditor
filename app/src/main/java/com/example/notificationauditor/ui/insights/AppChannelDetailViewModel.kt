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

class AppChannelDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val date: String = checkNotNull(savedStateHandle["date"])
    private val packageName: String = checkNotNull(savedStateHandle["packageName"])
    private val repository = NotificationRepository(AppDatabase.getInstance(application))

    private val _breakdown = MutableLiveData<List<InsightDetailListItem>>()
    val breakdown: LiveData<List<InsightDetailListItem>> = _breakdown

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val insight = repository.getInsightByDate(date) ?: return@launch
            _breakdown.value = parseBreakdown(insight.channelBreakdownJson)
                .filter { it.packageName == packageName }
                .sortedBy { it.utilityScore }
                .mapIndexed { index, channel ->
                    InsightDetailListItem.ChannelRow(index + 1, channel)
                }
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
