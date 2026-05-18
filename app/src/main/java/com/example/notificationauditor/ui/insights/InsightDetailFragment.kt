package com.example.notificationauditor.ui.insights

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
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
        val date = requireArguments().getString("date", "")
        val adapter = ChannelBreakdownAdapter { item ->
            findNavController().navigate(
                R.id.action_insight_detail_to_app_channel_detail,
                bundleOf("date" to date, "packageName" to item.packageName)
            )
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        viewModel.formattedDate.observe(viewLifecycleOwner) { formattedDate ->
            tvDate.text = formattedDate
        }

        viewModel.summary.observe(viewLifecycleOwner) { summary ->
            tvSummary.text = summary
        }

        viewModel.breakdown.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            rv.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}
