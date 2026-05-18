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

class InsightDetailFragment : Fragment() {

    private val viewModel: InsightDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_insight_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tvDate = view.findViewById<TextView>(R.id.tv_detail_date)
        val tvSummary = view.findViewById<TextView>(R.id.tv_detail_summary)
        val tvEmpty = view.findViewById<TextView>(R.id.tv_detail_empty)
        val rv = view.findViewById<RecyclerView>(R.id.rv_channels)
        val adapter = ChannelBreakdownAdapter()

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        viewModel.summary.observe(viewLifecycleOwner) { summary ->
            val parts = summary.split("  ·  ", limit = 2)
            tvDate.text = parts.getOrNull(0) ?: summary
            tvSummary.text = parts.getOrNull(1) ?: ""
        }

        viewModel.breakdown.observe(viewLifecycleOwner) { channels ->
            adapter.submitList(channels)
            tvEmpty.visibility = if (channels.isEmpty()) View.VISIBLE else View.GONE
            rv.visibility = if (channels.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}
