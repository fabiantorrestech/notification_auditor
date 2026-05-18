package com.example.notificationauditor.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
                        text = "• ${rec.packageName} / ${rec.channelId} — score: ${"%.2f".format(rec.utilityScore)}"
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

        view.findViewById<Button>(R.id.btn_new_study).setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Start New Study?")
                .setMessage("This will archive the current session and begin fresh tracking.")
                .setPositiveButton("Start") { _, _ -> viewModel.startNewStudy() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        view.findViewById<Button>(R.id.btn_clear_data).setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear Study Data?")
                .setMessage("All raw notification events for this session will be deleted. Insights history is preserved.")
                .setPositiveButton("Clear") { _, _ -> viewModel.clearStudyData() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        viewModel.refresh()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
