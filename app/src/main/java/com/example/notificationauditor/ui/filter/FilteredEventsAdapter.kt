package com.example.notificationauditor.ui.filter

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notificationauditor.R
import com.example.notificationauditor.data.db.entity.FilterRule
import com.example.notificationauditor.data.db.entity.NotificationEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FilteredEventsAdapter(
    private val pm: PackageManager
) : ListAdapter<NotificationEvent, FilteredEventsAdapter.ViewHolder>(DIFF) {

    private var rulesById: Map<Long, FilterRule> = emptyMap()
    private val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    fun setRules(rules: Map<Long, FilterRule>) {
        rulesById = rules
        notifyItemRangeChanged(0, itemCount)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvApp: TextView = view.findViewById(R.id.tv_event_app)
        val tvActionBadge: TextView = view.findViewById(R.id.tv_event_action_badge)
        val tvChannel: TextView = view.findViewById(R.id.tv_event_channel)
        val tvTime: TextView = view.findViewById(R.id.tv_event_time)
        val tvRule: TextView = view.findViewById(R.id.tv_event_rule)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_filtered_event, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = getItem(position)
        holder.tvApp.text = resolveAppName(event.packageName)
        holder.tvChannel.text = "Channel: ${event.channelId}"
        holder.tvTime.text = dateFormat.format(Date(event.postTimestamp))

        val rule = event.filteredByRuleId?.let { rulesById[it] }
        if (rule != null) {
            holder.tvActionBadge.text = rule.action
            holder.tvRule.text = "Rule #${rule.ruleId}: ${rule.ruleType} — \"${rule.pattern}\""
        } else {
            holder.tvActionBadge.text = "?"
            holder.tvRule.text = "Rule #${event.filteredByRuleId}"
        }
    }

    private fun resolveAppName(packageName: String): String =
        runCatching { pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString() }
            .getOrDefault(packageName)

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<NotificationEvent>() {
            override fun areItemsTheSame(a: NotificationEvent, b: NotificationEvent) = a.eventId == b.eventId
            override fun areContentsTheSame(a: NotificationEvent, b: NotificationEvent) = a == b
        }
    }
}
