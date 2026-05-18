package com.example.notificationauditor.ui.insights

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notificationauditor.R

class InsightsDashboardFragment : Fragment() {

    private val viewModel: InsightsDashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_insights_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rv_insights)
        val tvEmpty = view.findViewById<TextView>(R.id.tv_insights_empty)
        val adapter = DailyInsightAdapter()

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        viewModel.allInsights.observe(viewLifecycleOwner) { insights ->
            adapter.submitList(insights)
            tvEmpty.visibility = if (insights.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
