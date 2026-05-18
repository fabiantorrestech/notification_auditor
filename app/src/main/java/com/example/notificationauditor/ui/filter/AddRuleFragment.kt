package com.example.notificationauditor.ui.filter

import android.os.Bundle
import android.text.InputType
import android.text.method.KeyListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
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

    private var installedApps: List<InstalledAppOption> = emptyList()
    private var channelPickerState: ChannelPickerState = ChannelPickerState()
    private var channelKeyListener: KeyListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_add_rule, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val actvScope = view.findViewById<AutoCompleteTextView>(R.id.actv_rule_scope)
        val actvType = view.findViewById<AutoCompleteTextView>(R.id.actv_rule_type)
        val tilApp = view.findViewById<TextInputLayout>(R.id.til_app)
        val actvApp = view.findViewById<AutoCompleteTextView>(R.id.actv_app)
        val rowChannel = view.findViewById<LinearLayout>(R.id.row_channel)
        val tilChannel = view.findViewById<TextInputLayout>(R.id.til_channel_id)
        val actvChannel = view.findViewById<AutoCompleteTextView>(R.id.actv_channel_id)
        val tilPattern = view.findViewById<TextInputLayout>(R.id.til_pattern)
        val etPattern = view.findViewById<TextInputEditText>(R.id.et_pattern)
        val rgAction = view.findViewById<RadioGroup>(R.id.rg_action)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save_rule)
        val btnObserved = view.findViewById<MaterialButton>(R.id.btn_channel_observed)
        val btnAll = view.findViewById<MaterialButton>(R.id.btn_channel_all)
        val btnManual = view.findViewById<MaterialButton>(R.id.btn_channel_manual)

        channelKeyListener = actvChannel.keyListener
        actvChannel.threshold = 0
        actvChannel.setOnClickListener {
            if (actvChannel.isEnabled && channelPickerState.selectedMode != ChannelSourceMode.MANUAL) {
                actvChannel.showDropDown()
            }
        }

        val scopeOptions = listOf(SCOPE_ALL_APPS, SCOPE_APP, SCOPE_APP_CHANNEL)
        val ruleTypes = listOf(
            RuleType.MATCH_ALL,
            RuleType.KEYWORD,
            RuleType.REGEX,
            RuleType.CONTACT_BLACKLIST,
            RuleType.CONTACT_WHITELIST
        )

        actvScope.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, scopeOptions)
        )
        actvScope.setText(scopeOptions[0], false)
        actvType.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, ruleTypes)
        )
        actvType.setText(ruleTypes[0], false)

        viewModel.installedApps.observe(viewLifecycleOwner) { apps ->
            installedApps = apps
            actvApp.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, apps)
            )
        }

        viewModel.channelPickerState.observe(viewLifecycleOwner) { state ->
            channelPickerState = state
            syncChannelUi(
                selectedScope(actvScope),
                resolvePackageName(actvApp.text?.toString().orEmpty()),
                rowChannel,
                tilChannel,
                actvChannel,
                btnObserved,
                btnAll,
                btnManual
            )
        }

        actvScope.doAfterTextChanged {
            tilApp.error = null
            tilChannel.error = null
            syncScopeUi(
                actvScope,
                actvType,
                tilApp,
                actvApp,
                rowChannel,
                tilChannel,
                actvChannel,
                btnObserved,
                btnAll,
                btnManual,
                tilPattern,
                etPattern
            )
        }

        actvType.doAfterTextChanged {
            tilPattern.error = null
            syncPatternUi(actvType, tilPattern, etPattern)
        }

        actvApp.doAfterTextChanged {
            tilApp.error = null
            tilChannel.error = null
            if (selectedScope(actvScope) == SCOPE_APP_CHANNEL) {
                actvChannel.setText("", false)
                syncScopeUi(
                    actvScope,
                    actvType,
                    tilApp,
                    actvApp,
                    rowChannel,
                    tilChannel,
                    actvChannel,
                    btnObserved,
                    btnAll,
                    btnManual,
                    tilPattern,
                    etPattern
                )
            }
        }

        btnObserved.setOnClickListener {
            actvChannel.setText("", false)
            viewModel.selectChannelSourceMode(ChannelSourceMode.OBSERVED)
        }

        btnAll.setOnClickListener {
            actvChannel.setText("", false)
            viewModel.selectChannelSourceMode(ChannelSourceMode.ALL)
        }

        btnManual.setOnClickListener {
            actvChannel.setText("", false)
            viewModel.selectChannelSourceMode(ChannelSourceMode.MANUAL)
        }

        actvChannel.doAfterTextChanged {
            tilChannel.error = null
        }

        syncPatternUi(actvType, tilPattern, etPattern)
        syncScopeUi(
            actvScope,
            actvType,
            tilApp,
            actvApp,
            rowChannel,
            tilChannel,
            actvChannel,
            btnObserved,
            btnAll,
            btnManual,
            tilPattern,
            etPattern
        )

        btnSave.setOnClickListener {
            val scope = selectedScope(actvScope)
            val ruleType = actvType.text.toString()
            val pattern = if (ruleType == RuleType.MATCH_ALL) {
                ""
            } else {
                etPattern.text?.toString()?.trim().orEmpty()
            }
            val packageName = when (scope) {
                SCOPE_ALL_APPS -> null
                else -> resolvePackageName(actvApp.text?.toString()?.trim().orEmpty())
            }
            val channelId = when (scope) {
                SCOPE_APP_CHANNEL -> resolveChannelId(
                    actvChannel.text?.toString()?.trim().orEmpty(),
                    channelPickerState.selectedOptions()
                )
                else -> null
            }
            val action = when (rgAction.checkedRadioButtonId) {
                R.id.rb_suppress_exclude -> RuleAction.SUPPRESS_EXCLUDE
                R.id.rb_tag_only -> RuleAction.TAG_ONLY
                else -> RuleAction.EXCLUDE_ONLY
            }

            if (scope != SCOPE_ALL_APPS && packageName.isNullOrBlank()) {
                tilApp.error = "Select an installed app"
                return@setOnClickListener
            }

            if (scope == SCOPE_APP_CHANNEL && channelId.isNullOrBlank()) {
                tilChannel.error = "Channel ID is required for app channel scope"
                return@setOnClickListener
            }

            if (ruleType != RuleType.MATCH_ALL && pattern.isBlank()) {
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

    private fun selectedScope(scopeView: AutoCompleteTextView): String {
        val selected = scopeView.text?.toString().orEmpty()
        return if (selected in listOf(SCOPE_ALL_APPS, SCOPE_APP, SCOPE_APP_CHANNEL)) {
            selected
        } else {
            SCOPE_ALL_APPS
        }
    }

    private fun resolvePackageName(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null

        return installedApps.firstOrNull { option ->
            option.packageName == trimmed || option.toString() == trimmed
        }?.packageName ?: trimmed.takeIf { '.' in it }
    }

    private fun resolveChannelId(input: String, options: List<ChannelOption>): String? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null
        return if (channelPickerState.selectedMode == ChannelSourceMode.MANUAL) {
            trimmed
        } else {
            options.firstOrNull { option ->
                option.channelId == trimmed || option.toString() == trimmed
            }?.channelId
        }
    }

    private fun syncPatternUi(
        typeView: AutoCompleteTextView,
        patternLayout: TextInputLayout,
        patternInput: TextInputEditText
    ) {
        when (typeView.text?.toString()) {
            RuleType.MATCH_ALL -> {
                patternLayout.isVisible = false
                patternInput.setText("")
            }
            RuleType.CONTACT_BLACKLIST, RuleType.CONTACT_WHITELIST -> {
                patternLayout.isVisible = true
                patternLayout.hint = "Contact hash"
            }
            RuleType.REGEX -> {
                patternLayout.isVisible = true
                patternLayout.hint = "Regex pattern"
            }
            else -> {
                patternLayout.isVisible = true
                patternLayout.hint = "Keyword pattern"
            }
        }
    }

    private fun syncScopeUi(
        scopeView: AutoCompleteTextView,
        typeView: AutoCompleteTextView,
        appLayout: TextInputLayout,
        appView: AutoCompleteTextView,
        channelRow: LinearLayout,
        channelLayout: TextInputLayout,
        channelView: AutoCompleteTextView,
        btnObserved: MaterialButton,
        btnAll: MaterialButton,
        btnManual: MaterialButton,
        patternLayout: TextInputLayout,
        patternInput: TextInputEditText
    ) {
        val scope = selectedScope(scopeView)
        val appRequired = scope != SCOPE_ALL_APPS
        val packageName = resolvePackageName(appView.text?.toString().orEmpty())
        val channelVisible = scope == SCOPE_APP_CHANNEL

        appLayout.isVisible = appRequired
        channelRow.isVisible = channelVisible

        if (!channelVisible || packageName.isNullOrBlank()) {
            channelView.setText("", false)
            viewModel.onAppSelected(null)
        } else {
            viewModel.onAppSelected(packageName)
        }

        syncChannelUi(
            scope,
            packageName,
            channelRow,
            channelLayout,
            channelView,
            btnObserved,
            btnAll,
            btnManual
        )
        syncPatternUi(typeView, patternLayout, patternInput)
    }

    companion object {
        private const val SCOPE_ALL_APPS = "All apps"
        private const val SCOPE_APP = "Specific app"
        private const val SCOPE_APP_CHANNEL = "Specific app channel"
    }

    private fun syncChannelUi(
        scope: String,
        packageName: String?,
        channelRow: LinearLayout,
        channelLayout: TextInputLayout,
        channelView: AutoCompleteTextView,
        btnObserved: MaterialButton,
        btnAll: MaterialButton,
        btnManual: MaterialButton
    ) {
        val channelEnabled = scope == SCOPE_APP_CHANNEL && !packageName.isNullOrBlank()
        val selectedOptions = channelPickerState.selectedOptions()

        channelRow.alpha = if (channelEnabled) 1f else 0.6f
        channelLayout.isEnabled = channelEnabled
        channelView.isEnabled = channelEnabled
        btnObserved.isEnabled = channelEnabled
        btnManual.isEnabled = channelEnabled
        btnAll.isEnabled = channelEnabled && channelPickerState.isAllAvailable

        when (channelPickerState.selectedMode) {
            ChannelSourceMode.OBSERVED -> btnObserved.isChecked = true
            ChannelSourceMode.ALL -> btnAll.isChecked = true
            ChannelSourceMode.MANUAL -> btnManual.isChecked = true
        }

        if (channelPickerState.selectedMode == ChannelSourceMode.MANUAL) {
            channelLayout.hint = "Channel ID"
            channelView.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    emptyList<String>()
                )
            )
            channelView.keyListener = channelKeyListener
            channelView.inputType = InputType.TYPE_CLASS_TEXT
            channelView.isCursorVisible = true
        } else {
            channelLayout.hint = "Channel ID"
            channelView.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    selectedOptions
                )
            )
            channelView.keyListener = null
            channelView.inputType = InputType.TYPE_NULL
            channelView.isCursorVisible = false
        }

        channelLayout.helperText = buildChannelHelperText(scope, packageName, selectedOptions)
    }

    private fun buildChannelHelperText(
        scope: String,
        packageName: String?,
        selectedOptions: List<ChannelOption>
    ): String? = when {
        scope != SCOPE_APP_CHANNEL -> null
        packageName.isNullOrBlank() -> "Select an app first."
        channelPickerState.selectedMode == ChannelSourceMode.MANUAL ->
            "Enter the exact channel ID to save."
        channelPickerState.selectedMode == ChannelSourceMode.ALL && !channelPickerState.isAllAvailable ->
            channelPickerState.allUnavailableMessage
        channelPickerState.selectedMode == ChannelSourceMode.ALL && selectedOptions.isEmpty() ->
            "No channels were returned by the connected notification listener."
        channelPickerState.selectedMode == ChannelSourceMode.OBSERVED && selectedOptions.isEmpty() ->
            buildString {
                append("No observed channels yet.")
                channelPickerState.allUnavailableMessage?.let { message ->
                    append(' ')
                    append(message)
                } ?: append(" Switch to All or Manual.")
            }
        channelPickerState.selectedMode == ChannelSourceMode.ALL ->
            "Choose a channel from the connected notification listener."
        channelPickerState.allUnavailableMessage != null ->
            "Choose a previously seen channel. ${channelPickerState.allUnavailableMessage}"
        else -> "Choose a previously seen channel ID."
    }
}
