package com.example.notificationauditor.ui.dashboard

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.notificationauditor.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DashboardFragment : Fragment() {

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tvDuration = view.findViewById<TextView>(R.id.tv_study_duration)
        val tvTotal = view.findViewById<TextView>(R.id.tv_total_logged)
        val tvEmpty = view.findViewById<TextView>(R.id.tv_recommendations_empty)
        val llRecs = view.findViewById<LinearLayout>(R.id.ll_recommendations)

        viewModel.studyDurationDays.observe(viewLifecycleOwner) { days ->
            tvDuration.text = "Duration: $days day(s)"
        }
        viewModel.totalLogged.observe(viewLifecycleOwner) { count ->
            tvTotal.text = "Notifications logged: $count"
        }
        viewModel.recommendations.observe(viewLifecycleOwner) { recs ->
            llRecs.removeAllViews()
            if (recs.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
            } else {
                tvEmpty.visibility = View.GONE
                recs.take(5).forEach { rec ->
                    val chip = TextView(requireContext()).apply {
                        val appLabel = resolveAppName(requireContext().packageManager, rec.packageName)
                        text = "• $appLabel (${rec.packageName}) / ${rec.channelId} — score: ${"%.2f".format(rec.utilityScore)}"
                        textSize = 14f
                        setPadding(0, 4, 0, 4)
                    }
                    llRecs.addView(chip)
                }
            }
        }

        view.findViewById<Button>(R.id.btn_view_insights).setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_insights)
        }

        view.findViewById<Button>(R.id.btn_preview_analysis).setOnClickListener {
            viewModel.previewAnalysis()
        }

        viewModel.previewResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            val message = buildPreviewMessage(result)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Preview Analysis (unsaved)")
                .setMessage(message)
                .setPositiveButton("Dismiss", null)
                .setOnDismissListener { viewModel.clearPreviewResult() }
                .show()
        }

        view.findViewById<ImageButton>(R.id.ibtn_new_study).setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Start New Study?")
                .setMessage("This will archive the current session and begin fresh tracking.")
                .setPositiveButton("Start") { _, _ -> viewModel.startNewStudy() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        view.findViewById<ImageButton>(R.id.ibtn_clear_data).setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear Study Data?")
                .setMessage("All raw notification events for this session will be deleted. Insights history is preserved.")
                .setPositiveButton("Clear") { _, _ -> viewModel.clearStudyData() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        view.findViewById<ImageButton>(R.id.btn_info).setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("How Notification Auditor Works")
                .setMessage(buildInfoMessage())
                .setPositiveButton("Got it", null)
                .show()
        }

        viewModel.refresh()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun resolveAppName(pm: PackageManager, packageName: String): String =
        runCatching { pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString() }
            .getOrDefault(packageName)

    private fun buildInfoMessage(): String = """
All monitoring is done entirely on your device. No data is ever sent anywhere.

The app passively records when notifications arrive and how you interact with them, then scores each channel by utility.

━━ TERMINOLOGY ━━

Notification Channel
Each app can post to multiple channels (e.g. "Messages", "Promotions"). Auditor scores channels individually, not whole apps.

Utility Score (0.0 – 1.0)
How useful a channel is based on your behaviour:
  • High → you tap or open the app after these notifications
  • Low  → you swipe them away quickly or ignore them
Channels below 0.20 with 5+ notifications are flagged as suggested mutes.

Ghost Open
You received a notification and opened the app within 10 seconds — without tapping the notification itself. Counts as mild engagement.

Mass-Clear Dismissal
Clearing all notifications at once ("Clear all"). Weighted at 10% of a deliberate swipe — the app assumes you weren't specifically rejecting each one.

Suggested Mutes
To act on a suggestion: Settings → Apps → [App] → Notifications → disable the channel.

━━ DATA ━━

Raw events are kept for 30 days. Daily snapshots are kept for 1 year.

━━ PREVIEW vs RUN ANALYTICS ━━

Preview Analysis — runs scoring right now and shows results. Nothing is saved.
Run Analytics Now — saves today's snapshot to the database, identical to the nightly scheduled run.
""".trimIndent()

    private fun buildPreviewMessage(result: PreviewResult): String = buildString {
        appendLine("Last 24 hours")
        appendLine("Posted: ${result.totalPosted}  Clicks: ${result.clicks}  Dismissed: ${result.dismissals}  Ghost opens: ${result.ghostOpens}")
        appendLine()
        if (result.recommendations.isEmpty()) {
            append("No channels flagged. Keep collecting data.")
        } else {
            appendLine("Suggested mutes (7-day data):")
            result.recommendations.forEach { score ->
                appendLine("• ${score.packageName} / ${score.channelId}")
                append("  score: ${"%.2f".format(score.utilityScore)}  (${score.totalPosted} notifications)")
                appendLine()
            }
        }
    }
}
