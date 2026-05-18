package com.example.notificationauditor.ui.filter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notificationauditor.R
import com.example.notificationauditor.data.db.entity.FilterRule
import com.google.android.material.switchmaterial.SwitchMaterial

class FilterRuleAdapter(
    private val onToggle: (FilterRule, Boolean) -> Unit,
    private val onDelete: (FilterRule) -> Unit
) : ListAdapter<FilterRule, FilterRuleAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvType: TextView = view.findViewById(R.id.tv_rule_type)
        val tvAction: TextView = view.findViewById(R.id.tv_rule_action)
        val tvPattern: TextView = view.findViewById(R.id.tv_rule_pattern)
        val tvScope: TextView = view.findViewById(R.id.tv_rule_scope)
        val switch: SwitchMaterial = view.findViewById(R.id.switch_rule_enabled)
        val btnDelete: ImageButton = view.findViewById(R.id.ibtn_delete_rule)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_filter_rule, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rule = getItem(position)
        holder.tvType.text = rule.ruleType
        holder.tvAction.text = rule.action
        holder.tvPattern.text = rule.pattern
        holder.tvScope.text = buildScopeLabel(rule)

        holder.switch.setOnCheckedChangeListener(null)
        holder.switch.isChecked = rule.isEnabled
        holder.switch.setOnCheckedChangeListener { _, checked -> onToggle(rule, checked) }

        holder.btnDelete.setOnClickListener { onDelete(rule) }
    }

    private fun buildScopeLabel(rule: FilterRule): String = when {
        rule.packageName == null -> "All apps"
        rule.channelId == null -> rule.packageName
        else -> "${rule.packageName} / ${rule.channelId}"
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FilterRule>() {
            override fun areItemsTheSame(a: FilterRule, b: FilterRule) = a.ruleId == b.ruleId
            override fun areContentsTheSame(a: FilterRule, b: FilterRule) = a == b
        }
    }
}
