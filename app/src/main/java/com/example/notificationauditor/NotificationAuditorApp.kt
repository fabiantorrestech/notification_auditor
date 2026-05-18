package com.example.notificationauditor

import android.app.Application
import com.example.notificationauditor.data.db.AppDatabase
import com.example.notificationauditor.worker.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationAuditorApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initDatabase()
        WorkScheduler.scheduleAnalytics(this)
    }

    private fun initDatabase() {
        appScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            // Ensure at least one active session exists on first launch
            if (db.studySessionDao().getActiveSession() == null) {
                db.studySessionDao().insert(
                    com.example.notificationauditor.data.db.entity.StudySession()
                )
            }
        }
    }
}
