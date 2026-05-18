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
import com.example.notificationauditor.util.AppLabelResolver

class AppChannelDetailFragment : Fragment() {

    private val viewModel: AppChannelDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_app_channel_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tvAppName = view.findViewById<TextView>(R.id.tv_app_name)
        val tvPackageName = view.findViewById<TextView>(R.id.tv_package_name)
        val tvEmpty = view.findViewById<TextView>(R.id.tv_empty)
        val rv = view.findViewById<RecyclerView>(R.id.rv_channels)
        val adapter = ChannelBreakdownAdapter()

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        val packageName = requireArguments().getString("packageName", "")
        tvPackageName.text = packageName
        tvAppName.text = AppLabelResolver.resolve(requireContext(), packageName)

        viewModel.breakdown.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            rv.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}
