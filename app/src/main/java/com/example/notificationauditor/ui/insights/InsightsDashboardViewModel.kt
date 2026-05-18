package com.example.notificationauditor.ui.insights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.notificationauditor.data.db.AppDatabase
import com.example.notificationauditor.data.db.entity.DailyInsight
import com.example.notificationauditor.data.repository.NotificationRepository

class InsightsDashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = NotificationRepository(AppDatabase.getInstance(app))

    val allInsights: LiveData<List<DailyInsight>> = repository.observeAllInsights()
}
