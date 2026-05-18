package com.example.notificationauditor.ui.insights

import android.content.Intent
import android.graphics.Color
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notificationauditor.R
import com.example.notificationauditor.util.AppIconResolver
import com.example.notificationauditor.util.AppLabelResolver
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.google.android.material.progressindicator.LinearProgressIndicator

class ChannelBreakdownAdapter(
    private val onItemClick: ((ChannelBreakdown) -> Unit)? = null
) : ListAdapter<InsightDetailListItem, RecyclerView.ViewHolder>(DiffCallback) {

    class ChannelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tv_rank)
        val ivAppIcon: ImageView = view.findViewById(R.id.iv_app_icon)
        val tvAppName: TextView = view.findViewById(R.id.tv_app_name)
        val tvChannelId: TextView = view.findViewById(R.id.tv_channel_id)
        val tvStats: TextView = view.findViewById(R.id.tv_channel_stats)
        val progress: LinearProgressIndicator = view.findViewById(R.id.progress_utility)
        val tvScoreLabel: TextView = view.findViewById(R.id.tv_score_label)
        val chipMute: Chip = view.findViewById(R.id.chip_mute)
        val btnOpenSettings: ImageButton = view.findViewById(R.id.btn_open_settings)
    }

    class SectionHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_section_title)
    }

    class DividerViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is InsightDetailListItem.SectionHeader -> VIEW_TYPE_HEADER
        is InsightDetailListItem.Divider -> VIEW_TYPE_DIVIDER
        is InsightDetailListItem.ChannelRow -> VIEW_TYPE_CHANNEL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> SectionHeaderViewHolder(
                inflater.inflate(R.layout.item_insight_section_header, parent, false)
            )
            VIEW_TYPE_DIVIDER -> DividerViewHolder(
                inflater.inflate(R.layout.item_insight_divider, parent, false)
            )
            else -> ChannelViewHolder(
                inflater.inflate(R.layout.item_channel_breakdown, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is InsightDetailListItem.SectionHeader -> {
                (holder as SectionHeaderViewHolder).tvTitle.text = item.title
            }
            is InsightDetailListItem.Divider -> Unit
            is InsightDetailListItem.ChannelRow -> bindChannelRow(holder as ChannelViewHolder, item)
        }
    }

    private fun bindChannelRow(holder: ChannelViewHolder, item: InsightDetailListItem.ChannelRow) {
        val channel = item.channel
        val ctx = holder.itemView.context

        holder.tvRank.text = "${item.rank}."
        holder.ivAppIcon.setImageDrawable(AppIconResolver.resolve(ctx, channel.packageName))
        holder.tvAppName.text = AppLabelResolver.resolve(ctx, channel.packageName)
        holder.tvChannelId.text = "${channel.packageName} · ${channel.channelId}"

        holder.tvStats.text = buildString {
            append("${channel.totalPosted} posted")
            append(" · ${channel.clicks} clicks")
            append(" · ${channel.dismissals} dismissed")
            if (channel.ghostOpens > 0) append(" · ${channel.ghostOpens} ghost opens")
        }

        val scorePercent = (channel.utilityScore * 100).toInt().coerceIn(0, 100)
        holder.progress.setProgressCompat(scorePercent, false)
        holder.tvScoreLabel.text = "Utility score: ${"%.2f".format(channel.utilityScore)}"

        // Use system attrs (android.R.attr) to avoid R class resolution issues with library attrs.
        // Flagged channels (score < 0.20) get colorError (red), everything else gets colorPrimary.
        val colorAttr = if (channel.flagged) android.R.attr.colorError else android.R.attr.colorPrimary
        val color = MaterialColors.getColor(holder.itemView, colorAttr, Color.GRAY)
        holder.progress.setIndicatorColor(color)

        holder.chipMute.visibility = if (channel.flagged) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onItemClick?.invoke(channel) }

        holder.btnOpenSettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, channel.packageName)
            }
            ctx.startActivity(intent)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<InsightDetailListItem>() {
        override fun areItemsTheSame(old: InsightDetailListItem, new: InsightDetailListItem): Boolean {
            return when {
                old is InsightDetailListItem.SectionHeader && new is InsightDetailListItem.SectionHeader ->
                    old.title == new.title
                old is InsightDetailListItem.Divider && new is InsightDetailListItem.Divider -> true
                old is InsightDetailListItem.ChannelRow && new is InsightDetailListItem.ChannelRow ->
                    old.channel.packageName == new.channel.packageName &&
                        old.channel.channelId == new.channel.channelId &&
                        old.rank == new.rank
                else -> false
            }
        }

        override fun areContentsTheSame(old: InsightDetailListItem, new: InsightDetailListItem) = old == new
    }

    private companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_DIVIDER = 1
        const val VIEW_TYPE_CHANNEL = 2
    }
}
