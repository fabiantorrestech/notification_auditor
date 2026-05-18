package com.example.notificationauditor.ui.insights

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.notificationauditor.R
import com.example.notificationauditor.worker.AnalyticsWorker
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class InsightsDashboardFragment : Fragment() {

    private val viewModel: InsightsDashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_insights_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rv_insights)
        val tvEmpty = view.findViewById<TextView>(R.id.tv_insights_empty)
        val adapter = DailyInsightAdapter { date ->
            findNavController().navigate(
                R.id.action_insights_to_detail,
                bundleOf("date" to date)
            )
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        viewModel.allInsights.observe(viewLifecycleOwner) { insights ->
            adapter.submitList(insights)
            tvEmpty.visibility = if (insights.isEmpty()) View.VISIBLE else View.GONE
        }

        view.findViewById<ImageButton>(R.id.ibtn_run_analytics).setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Run Analytics Engine?")
                .setMessage("This runs the full analytics engine and saves a DailyInsight row to the database — identical to the nightly scheduled run.")
                .setPositiveButton("Run") { _, _ ->
                    WorkManager.getInstance(requireContext())
                        .enqueue(OneTimeWorkRequest.from(AnalyticsWorker::class.java))
                    Toast.makeText(requireContext(), "Analytics worker enqueued", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
