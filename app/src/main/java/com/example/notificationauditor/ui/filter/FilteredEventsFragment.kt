package com.example.notificationauditor.ui.filter

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

class FilteredEventsFragment : Fragment() {

    private val viewModel: FilteredEventsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_filtered_events, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rv_filtered_events)
        val tvEmpty = view.findViewById<TextView>(R.id.tv_filtered_empty)

        val adapter = FilteredEventsAdapter(requireContext().packageManager)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        viewModel.allRulesById.observe(viewLifecycleOwner) { rules ->
            adapter.setRules(rules)
        }

        viewModel.filteredEvents.observe(viewLifecycleOwner) { events ->
            adapter.submitList(events)
            tvEmpty.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
