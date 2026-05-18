package com.example.notificationauditor.ui.settings

import android.content.Context
import android.text.format.DateFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.notificationauditor.R
import com.example.notificationauditor.util.AppPrefs
import com.example.notificationauditor.util.DailyCutoff
import com.example.notificationauditor.util.DailyCutoffTime
import com.example.notificationauditor.worker.WorkScheduler
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val prefs = requireContext().getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
        val slider = view.findViewById<Slider>(R.id.slider_threshold)
        val tvValue = view.findViewById<TextView>(R.id.tv_threshold_value)
        val cardCutoff = view.findViewById<MaterialCardView>(R.id.card_daily_cutoff)
        val tvCutoffValue = view.findViewById<TextView>(R.id.tv_cutoff_value)
        val tvCutoffSummary = view.findViewById<TextView>(R.id.tv_cutoff_summary)

        val current = prefs.getFloat(AppPrefs.KEY_THRESHOLD, AppPrefs.DEFAULT_THRESHOLD)
        slider.value = current.coerceIn(slider.valueFrom, slider.valueTo)
        tvValue.text = "%.2f".format(current)
        bindCutoffViews(tvCutoffValue, tvCutoffSummary)

        slider.addOnChangeListener { _, value, _ ->
            tvValue.text = "%.2f".format(value)
            prefs.edit().putFloat(AppPrefs.KEY_THRESHOLD, value).apply()
        }

        cardCutoff.setOnClickListener {
            showCutoffTimePicker()
        }
    }

    private fun bindCutoffViews(tvCutoffValue: TextView, tvCutoffSummary: TextView) {
        val cutoff = DailyCutoffTime.load(requireContext())
        tvCutoffValue.text = DailyCutoffTime.format(requireContext(), cutoff)
        tvCutoffSummary.text =
            "Daily cutoff boundary. Android may run analysis later if charging or idle constraints are not yet met."
    }

    private fun showCutoffTimePicker() {
        val context = requireContext()
        val current = DailyCutoffTime.load(context)
        val picker = MaterialTimePicker.Builder()
            .setHour(current.hour)
            .setMinute(current.minute)
            .setTitleText("Daily analysis cutoff time")
            .setTimeFormat(
                if (DateFormat.is24HourFormat(context)) TimeFormat.CLOCK_24H
                else TimeFormat.CLOCK_12H
            )
            .build()

        picker.addOnPositiveButtonClickListener {
            DailyCutoffTime.save(context, DailyCutoff(picker.hour, picker.minute))
            WorkScheduler.rescheduleAnalytics(context)
            view?.let { root ->
                bindCutoffViews(
                    root.findViewById(R.id.tv_cutoff_value),
                    root.findViewById(R.id.tv_cutoff_summary)
                )
            }
        }

        picker.show(childFragmentManager, "daily_cutoff_time")
    }
}
