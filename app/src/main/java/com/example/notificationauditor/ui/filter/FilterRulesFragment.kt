package com.example.notificationauditor.ui.filter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notificationauditor.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FilterRulesFragment : Fragment() {

    private val viewModel: FilterRulesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_filter_rules, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rv_filter_rules)
        val tvEmpty = view.findViewById<TextView>(R.id.tv_rules_empty)
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_add_rule)
        val btnCaught = view.findViewById<MaterialButton>(R.id.btn_view_filtered_events)

        val adapter = FilterRuleAdapter(
            onToggle = { rule, enabled -> viewModel.toggleRule(rule, enabled) },
            onDelete = { rule ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Rule?")
                    .setMessage("\"${rule.pattern}\" will be removed.")
                    .setPositiveButton("Delete") { _, _ -> viewModel.deleteRule(rule) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        viewModel.allRules.observe(viewLifecycleOwner) { rules ->
            adapter.submitList(rules)
            tvEmpty.visibility = if (rules.isEmpty()) View.VISIBLE else View.GONE
        }

        fab.setOnClickListener {
            findNavController().navigate(R.id.action_filter_rules_to_add_rule)
        }

        btnCaught.setOnClickListener {
            findNavController().navigate(R.id.action_filter_rules_to_filtered_events)
        }
    }
}
