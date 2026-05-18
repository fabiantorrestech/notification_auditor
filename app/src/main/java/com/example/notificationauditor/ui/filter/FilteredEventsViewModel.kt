package com.example.notificationauditor.ui.filter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import com.example.notificationauditor.data.db.AppDatabase
import com.example.notificationauditor.data.db.entity.FilterRule
import com.example.notificationauditor.data.repository.NotificationRepository
import kotlinx.coroutines.flow.map

class FilteredEventsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = NotificationRepository(AppDatabase.getInstance(app))

    val filteredEvents = repository.observeFilteredEvents().asLiveData()

    val allRulesById = repository.observeAllRules()
        .map { list -> list.associateBy { it.ruleId } }
        .asLiveData()
}
