package com.example.notificationauditor.ui.insights

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notificationauditor.R
import com.example.notificationauditor.data.db.entity.DailyInsight
import com.example.notificationauditor.util.AppIconResolver
import com.example.notificationauditor.util.AppLabelResolver
import com.example.notificationauditor.util.InsightDateFormatter
import org.json.JSONArray

class DailyInsightAdapter(
    private val onItemClick: (date: String) -> Unit = {}
) : ListAdapter<DailyInsight, DailyInsightAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tv_date)
        val tvStats: TextView = view.findViewById(R.id.tv_stats)
        val tvRecommendationsHeader: TextView = view.findViewById(R.id.tv_recommendations_header)
        val recommendationsContainer: LinearLayout = view.findViewById(R.id.container_recommendations)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_daily_insight, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val insight = getItem(position)
        holder.itemView.setOnClickListener { onItemClick(insight.date) }
        holder.tvDate.text = "${position + 1}. ${InsightDateFormatter.format(insight.date)}"
        holder.tvStats.text = buildString {
            append("Posted: ${insight.totalPosted}")
            append("  Clicks: ${insight.clicks}")
            append("  Dismissed: ${insight.dismissals}")
            if (insight.ghostOpens > 0) append("  Ghost opens: ${insight.ghostOpens}")
        }

        val recs = parseRecommendations(insight.recommendationsJson)
        if (recs.isNotEmpty()) {
            holder.tvRecommendationsHeader.visibility = View.VISIBLE
            holder.recommendationsContainer.visibility = View.VISIBLE
            val context = holder.itemView.context
            holder.recommendationsContainer.removeAllViews()
            val inflater = LayoutInflater.from(context)
            recs.forEach { (packageName, channelId) ->
                val itemView = inflater.inflate(
                    R.layout.item_insight_suggestion,
                    holder.recommendationsContainer,
                    false
                )
                itemView.findViewById<ImageView>(R.id.iv_suggestion_icon)
                    .setImageDrawable(AppIconResolver.resolve(context, packageName))
                itemView.findViewById<TextView>(R.id.tv_suggestion_title).text =
                    AppLabelResolver.resolve(context, packageName)
                itemView.findViewById<TextView>(R.id.tv_suggestion_subtitle).text =
                    "$packageName · $channelId"
                holder.recommendationsContainer.addView(itemView)
            }
        } else {
            holder.tvRecommendationsHeader.visibility = View.GONE
            holder.recommendationsContainer.visibility = View.GONE
            holder.recommendationsContainer.removeAllViews()
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
