package com.example.notificationauditor.ui.insights

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notificationauditor.R
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.google.android.material.progressindicator.LinearProgressIndicator

class ChannelBreakdownAdapter :
    ListAdapter<ChannelBreakdown, ChannelBreakdownAdapter.ViewHolder>(DiffCallback) {

    private val appNameCache = HashMap<String, String>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAppName: TextView = view.findViewById(R.id.tv_app_name)
        val tvChannelId: TextView = view.findViewById(R.id.tv_channel_id)
        val tvStats: TextView = view.findViewById(R.id.tv_channel_stats)
        val progress: LinearProgressIndicator = view.findViewById(R.id.progress_utility)
        val tvScoreLabel: TextView = view.findViewById(R.id.tv_score_label)
        val chipMute: Chip = view.findViewById(R.id.chip_mute)
        val btnOpenSettings: ImageButton = view.findViewById(R.id.btn_open_settings)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_channel_breakdown, parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val ctx = holder.itemView.context

        holder.tvAppName.text = resolveAppName(ctx.packageManager, item.packageName)
        holder.tvChannelId.text = "${item.packageName} · ${item.channelId}"

        holder.tvStats.text = buildString {
            append("${item.totalPosted} posted")
            append(" · ${item.clicks} clicks")
            append(" · ${item.dismissals} dismissed")
            if (item.ghostOpens > 0) append(" · ${item.ghostOpens} ghost opens")
        }

        val scorePercent = (item.utilityScore * 100).toInt().coerceIn(0, 100)
        holder.progress.setProgressCompat(scorePercent, false)
        holder.tvScoreLabel.text = "Utility score: ${"%.2f".format(item.utilityScore)}"

        // Use system attrs (android.R.attr) to avoid R class resolution issues with library attrs.
        // Flagged channels (score < 0.20) get colorError (red), everything else gets colorPrimary.
        val colorAttr = if (item.flagged) android.R.attr.colorError else android.R.attr.colorPrimary
        val color = MaterialColors.getColor(holder.itemView, colorAttr, Color.GRAY)
        holder.progress.setIndicatorColor(color)

        holder.chipMute.visibility = if (item.flagged) View.VISIBLE else View.GONE

        holder.btnOpenSettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, item.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, item.channelId)
            }
            ctx.startActivity(intent)
        }
    }

    private fun resolveAppName(pm: PackageManager, packageName: String): String {
        return appNameCache.getOrPut(packageName) {
            runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            }.getOrDefault(packageName)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ChannelBreakdown>() {
        override fun areItemsTheSame(old: ChannelBreakdown, new: ChannelBreakdown) =
            old.packageName == new.packageName && old.channelId == new.channelId
        override fun areContentsTheSame(old: ChannelBreakdown, new: ChannelBreakdown) = old == new
    }
}
