package com.example.notificationauditor.ui.filter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.notificationauditor.data.db.AppDatabase
import com.example.notificationauditor.data.db.entity.FilterRule
import com.example.notificationauditor.data.repository.NotificationRepository
import kotlinx.coroutines.launch

class AddRuleViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = NotificationRepository(AppDatabase.getInstance(app))

    fun addRule(
        ruleType: String,
        pattern: String,
        packageName: String?,
        channelId: String?,
        action: String
    ) {
        viewModelScope.launch {
            repository.insertRule(
                FilterRule(
                    ruleType = ruleType,
                    pattern = pattern,
                    packageName = packageName?.takeIf { it.isNotBlank() },
                    channelId = channelId?.takeIf { it.isNotBlank() },
                    action = action
                )
            )
        }
    }
}
