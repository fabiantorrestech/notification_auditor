package com.example.notificationauditor.ui.insights

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notificationauditor.R
import com.example.notificationauditor.data.db.entity.DailyInsight
import org.json.JSONArray

class DailyInsightAdapter :
    ListAdapter<DailyInsight, DailyInsightAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tv_date)
        val tvStats: TextView = view.findViewById(R.id.tv_stats)
        val tvRecommendations: TextView = view.findViewById(R.id.tv_recommendations)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_daily_insight, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val insight = getItem(position)
        holder.tvDate.text = insight.date
        holder.tvStats.text = buildString {
            append("Posted: ${insight.totalPosted}")
            append("  Clicks: ${insight.clicks}")
            append("  Dismissed: ${insight.dismissals}")
            if (insight.ghostOpens > 0) append("  Ghost opens: ${insight.ghostOpens}")
        }

        val recs = parseRecommendations(insight.recommendationsJson)
        if (recs.isNotEmpty()) {
            holder.tvRecommendations.visibility = View.VISIBLE
            holder.tvRecommendations.text = "Suggested mutes: " + recs.joinToString(", ") {
                "${it.first}/${it.second}"
            }
        } else {
            holder.tvRecommendations.visibility = View.GONE
        }
    }

    private fun parseRecommendations(json: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return result
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(obj.getString("packageName") to obj.getString("channelId"))
        }
        return result
    }

    private object DiffCallback : DiffUtil.ItemCallback<DailyInsight>() {
        override fun areItemsTheSame(old: DailyInsight, new: DailyInsight) = old.date == new.date
        override fun areContentsTheSame(old: DailyInsight, new: DailyInsight) = old == new
    }
}
