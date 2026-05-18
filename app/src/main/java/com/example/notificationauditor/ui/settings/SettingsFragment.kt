package com.example.notificationauditor.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.notificationauditor.R
import com.example.notificationauditor.util.AppPrefs
import com.google.android.material.slider.Slider

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val prefs = requireContext().getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
        val slider = view.findViewById<Slider>(R.id.slider_threshold)
        val tvValue = view.findViewById<TextView>(R.id.tv_threshold_value)

        val current = prefs.getFloat(AppPrefs.KEY_THRESHOLD, AppPrefs.DEFAULT_THRESHOLD)
        slider.value = current.coerceIn(slider.valueFrom, slider.valueTo)
        tvValue.text = "%.2f".format(current)

        slider.addOnChangeListener { _, value, _ ->
            tvValue.text = "%.2f".format(value)
            prefs.edit().putFloat(AppPrefs.KEY_THRESHOLD, value).apply()
        }
    }
}
