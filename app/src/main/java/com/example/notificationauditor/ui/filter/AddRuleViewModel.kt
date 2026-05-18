package com.example.notificationauditor.ui.filter

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.notificationauditor.data.db.AppDatabase
import com.example.notificationauditor.data.db.entity.FilterRule
import com.example.notificationauditor.data.db.entity.RuleAction
import com.example.notificationauditor.data.repository.NotificationRepository
import com.example.notificationauditor.service.NotificationHarvesterService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

data class InstalledAppOption(
    val packageName: String,
    val appLabel: String
) {
    override fun toString(): String = if (appLabel == packageName) {
        packageName
    } else {
        "$appLabel ($packageName)"
    }
}

data class ChannelOption(
    val channelId: String,
    val displayName: String? = null,
    val sourceLabel: String? = null
) {
    override fun toString(): String = when {
        !displayName.isNullOrBlank() && displayName != channelId -> "$displayName ($channelId)"
        !sourceLabel.isNullOrBlank() -> "$channelId ($sourceLabel)"
        else -> channelId
    }
}

enum class ChannelSourceMode {
    OBSERVED,
    ALL,
    MANUAL
}

data class ChannelPickerState(
    val selectedMode: ChannelSourceMode = ChannelSourceMode.OBSERVED,
    val observedOptions: List<ChannelOption> = emptyList(),
    val allOptions: List<ChannelOption> = emptyList(),
    val isAllAvailable: Boolean = false,
    val allUnavailableMessage: String? = null
) {
    fun selectedOptions(): List<ChannelOption> = when (selectedMode) {
        ChannelSourceMode.OBSERVED -> observedOptions
        ChannelSourceMode.ALL -> if (isAllAvailable) allOptions else emptyList()
        ChannelSourceMode.MANUAL -> emptyList()
    }
}

class AddRuleViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = NotificationRepository(AppDatabase.getInstance(app))
    private val packageManager = app.packageManager

    private val _installedApps = MutableLiveData<List<InstalledAppOption>>(emptyList())
    val installedApps: LiveData<List<InstalledAppOption>> = _installedApps

    private val _channelPickerState = MutableLiveData(ChannelPickerState())
    val channelPickerState: LiveData<ChannelPickerState> = _channelPickerState

    init {
        loadInstalledApps()
    }

    fun onAppSelected(packageName: String?) {
        if (packageName.isNullOrBlank()) {
            _channelPickerState.value = ChannelPickerState()
            return
        }

        _channelPickerState.value = ChannelPickerState(selectedMode = ChannelSourceMode.OBSERVED)

        viewModelScope.launch(Dispatchers.IO) {
            val observedChannels = linkedMapOf<String, ChannelOption>()

            repository.getObservedChannelIds(packageName).forEach { channelId ->
                observedChannels.putIfAbsent(
                    channelId,
                    ChannelOption(channelId = channelId, sourceLabel = "Observed")
                )
            }

            extractInsightChannelIds(packageName).forEach { channelId ->
                observedChannels.putIfAbsent(
                    channelId,
                    ChannelOption(channelId = channelId, sourceLabel = "Observed")
                )
            }

            val allChannels = NotificationHarvesterService.getNotificationChannelsForPackage(packageName)
            _channelPickerState.postValue(
                ChannelPickerState(
                    selectedMode = ChannelSourceMode.OBSERVED,
                    observedOptions = observedChannels.values
                        .sortedBy { it.channelId.lowercase() },
                    allOptions = allChannels.channels.map { channel ->
                        ChannelOption(
                            channelId = channel.channelId,
                            displayName = channel.channelName
                        )
                    },
                    isAllAvailable = allChannels.isAvailable,
                    allUnavailableMessage = allChannels.unavailableMessage
                )
            )
        }
    }

    fun selectChannelSourceMode(mode: ChannelSourceMode) {
        val current = _channelPickerState.value ?: ChannelPickerState()
        if (mode == ChannelSourceMode.ALL && !current.isAllAvailable) {
            return
        }
        _channelPickerState.value = current.copy(selectedMode = mode)
    }

    fun addRule(
        ruleType: String,
        pattern: String,
        packageName: String?,
        channelId: String?,
        action: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertRule(
                FilterRule(
                    ruleType = ruleType,
                    pattern = pattern,
                    packageName = packageName?.takeIf { it.isNotBlank() },
                    channelId = channelId?.takeIf { it.isNotBlank() },
                    action = RuleAction.normalize(action)
                )
            )
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledApplications(0)
            }

            _installedApps.postValue(
                apps
                .map { info ->
                    InstalledAppOption(
                        packageName = info.packageName,
                        appLabel = runCatching {
                            packageManager.getApplicationLabel(info).toString()
                        }.getOrDefault(info.packageName)
                    )
                }
                .sortedWith(
                    compareBy<InstalledAppOption> { it.appLabel.lowercase() }
                        .thenBy { it.packageName.lowercase() }
                )
            )
        }
    }

    private suspend fun extractInsightChannelIds(packageName: String): List<String> {
        val channelIds = linkedSetOf<String>()
        repository.getAllInsightsSnapshot().forEach { insight ->
            collectChannelIds(insight.channelBreakdownJson, packageName, channelIds)
            collectChannelIds(insight.recommendationsJson, packageName, channelIds)
        }
        return channelIds.toList()
    }

    private fun collectChannelIds(
        json: String,
        packageName: String,
        channelIds: MutableSet<String>
    ) {
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            if (item.optString("packageName") != packageName) continue
            val channelId = item.optString("channelId")
            if (channelId.isNotBlank()) {
                channelIds += channelId
            }
        }
    }
}
