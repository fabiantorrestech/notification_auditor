package com.example.notificationauditor.ui.filter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.notificationauditor.data.db.AppDatabase
import com.example.notificationauditor.data.db.entity.FilterRule
import com.example.notificationauditor.data.repository.NotificationRepository
import kotlinx.coroutines.launch

class FilterRulesViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = NotificationRepository(AppDatabase.getInstance(app))

    val allRules = repository.observeAllRules().asLiveData()

    fun toggleRule(rule: FilterRule, enabled: Boolean) {
        viewModelScope.launch { repository.updateRule(rule.copy(isEnabled = enabled)) }
    }

    fun deleteRule(rule: FilterRule) {
        viewModelScope.launch { repository.deleteRule(rule) }
    }
}
