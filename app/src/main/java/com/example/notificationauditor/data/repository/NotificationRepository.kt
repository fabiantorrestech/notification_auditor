package com.example.notificationauditor.data.repository

import androidx.lifecycle.LiveData
import com.example.notificationauditor.data.db.AppDatabase
import com.example.notificationauditor.data.db.dao.ChannelKey
import com.example.notificationauditor.data.db.entity.DailyInsight
import com.example.notificationauditor.data.db.entity.FilterRule
import com.example.notificationauditor.data.db.entity.NotificationEvent
import com.example.notificationauditor.data.db.entity.StudySession
import kotlinx.coroutines.flow.Flow

class NotificationRepository(db: AppDatabase) {

    private val sessionDao = db.studySessionDao()
    private val eventDao = db.notificationEventDao()
    private val insightDao = db.dailyInsightDao()
    private val filterRuleDao = db.filterRuleDao()

    // --- StudySession ---

    suspend fun getActiveSession(): StudySession? = sessionDao.getActiveSession()

    fun observeActiveSession(): LiveData<StudySession?> = sessionDao.observeActiveSession()

    suspend fun startNewSession(): Long {
        sessionDao.deactivateAll()
        return sessionDao.insert(StudySession())
    }

    suspend fun endActiveSession() = sessionDao.deactivateAll()

    // --- NotificationEvent ---

    suspend fun insertEvent(event: NotificationEvent): Long = eventDao.insert(event)

    suspend fun updateEvent(event: NotificationEvent) = eventDao.update(event)

    suspend fun getLatestUnresolved(packageName: String, channelId: String): NotificationEvent? =
        eventDao.getLatestUnresolved(packageName, channelId)

    suspend fun getEventsInWindow(sessionId: Long, from: Long, to: Long): List<NotificationEvent> =
        eventDao.getEventsInWindow(sessionId, from, to)

    suspend fun getChannelEventsInWindow(
        sessionId: Long,
        pkg: String,
        channelId: String,
        from: Long,
        to: Long
    ): List<NotificationEvent> = eventDao.getChannelEventsInWindow(sessionId, pkg, channelId, from, to)

    suspend fun countEventsForSession(sessionId: Long): Int = eventDao.countForSession(sessionId)

    suspend fun deleteEventsForSession(sessionId: Long) = eventDao.deleteForSession(sessionId)

    suspend fun pruneRawEvents(cutoffMs: Long) = eventDao.pruneEventsBefore(cutoffMs)

    suspend fun getObservedChannelIds(packageName: String): List<String> =
        eventDao.getObservedChannelIds(packageName)

    suspend fun getActiveChannels(sessionId: Long, from: Long, to: Long): List<ChannelKey> =
        eventDao.getActiveChannels(sessionId, from, to)

    // --- DailyInsight ---

    suspend fun saveInsight(insight: DailyInsight) = insightDao.insertOrReplace(insight)

    fun observeAllInsights(): LiveData<List<DailyInsight>> = insightDao.observeAll()

    suspend fun getInsightsInRange(from: String, to: String): List<DailyInsight> =
        insightDao.getInRange(from, to)

    suspend fun getLatestInsight(): DailyInsight? = insightDao.getLatest()

    suspend fun getInsightByDate(date: String): DailyInsight? = insightDao.getByDate(date)

    suspend fun getAllInsightsSnapshot(): List<DailyInsight> = insightDao.getAllSnapshot()

    suspend fun pruneOldInsights(cutoffDate: String) = insightDao.pruneOlderThan(cutoffDate)

    // --- FilterRule ---

    suspend fun insertRule(rule: FilterRule): Long = filterRuleDao.insert(rule)

    suspend fun updateRule(rule: FilterRule) = filterRuleDao.update(rule)

    suspend fun deleteRule(rule: FilterRule) = filterRuleDao.delete(rule)

    fun observeAllRules(): Flow<List<FilterRule>> = filterRuleDao.observeAll()

    fun observeEnabledRules(): Flow<List<FilterRule>> = filterRuleDao.observeEnabled()

    fun observeFilteredEvents(): Flow<List<NotificationEvent>> = eventDao.observeFiltered()
}
