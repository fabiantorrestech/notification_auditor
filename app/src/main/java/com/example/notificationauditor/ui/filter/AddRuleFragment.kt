package com.example.notificationauditor.ui.filter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.notificationauditor.R
import com.example.notificationauditor.data.db.entity.RuleAction
import com.example.notificationauditor.data.db.entity.RuleType
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class AddRuleFragment : Fragment() {

    private val viewModel: AddRuleViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_add_rule, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val actvType = view.findViewById<AutoCompleteTextView>(R.id.actv_rule_type)
        val tilPattern = view.findViewById<TextInputLayout>(R.id.til_pattern)
        val etPattern = view.findViewById<TextInputEditText>(R.id.et_pattern)
        val etPackage = view.findViewById<TextInputEditText>(R.id.et_package_name)
        val etChannel = view.findViewById<TextInputEditText>(R.id.et_channel_id)
        val rgAction = view.findViewById<RadioGroup>(R.id.rg_action)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save_rule)

        val ruleTypes = listOf(RuleType.KEYWORD, RuleType.REGEX, RuleType.CONTACT_BLACKLIST, RuleType.CONTACT_WHITELIST)
        actvType.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, ruleTypes)
        )
        actvType.setText(ruleTypes[0], false)

        btnSave.setOnClickListener {
            val ruleType = actvType.text.toString()
            val pattern = etPattern.text?.toString()?.trim() ?: ""
            val packageName = etPackage.text?.toString()?.trim()
            val channelId = etChannel.text?.toString()?.trim()
            val action = if (rgAction.checkedRadioButtonId == R.id.rb_suppress) RuleAction.SUPPRESS else RuleAction.FLAG

            if (pattern.isBlank()) {
                tilPattern.error = "Pattern cannot be empty"
                return@setOnClickListener
            }

            if (ruleType == RuleType.REGEX) {
                try {
                    Regex(pattern)
                } catch (e: Exception) {
                    tilPattern.error = "Invalid regex: ${e.message}"
                    return@setOnClickListener
                }
            }

            tilPattern.error = null
            viewModel.addRule(ruleType, pattern, packageName, channelId, action)
            findNavController().popBackStack()
        }
    }
}
