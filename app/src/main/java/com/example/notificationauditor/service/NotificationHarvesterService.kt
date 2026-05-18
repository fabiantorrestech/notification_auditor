package com.example.notificationauditor.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.notificationauditor.data.db.AppDatabase
import com.example.notificationauditor.data.db.entity.NotificationEvent
import com.example.notificationauditor.data.repository.NotificationRepository
import com.example.notificationauditor.util.DndTracker
import com.example.notificationauditor.util.GhostOpenDetector
import com.example.notificationauditor.util.NotificationFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.security.MessageDigest

class NotificationHarvesterService : NotificationListenerService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var repository: NotificationRepository
    private lateinit var dndTracker: DndTracker
    private lateinit var ghostOpenDetector: GhostOpenDetector

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(applicationContext)
        repository = NotificationRepository(db)
        dndTracker = DndTracker(applicationContext)
        ghostOpenDetector = GhostOpenDetector(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        if (NotificationFilter.shouldIgnore(sbn)) return

        val isIntercepted = dndTracker.isInterceptedByDnd(sbn.key, rankingMap)
        val senderName = NotificationFilter.extractSenderName(sbn)
        val contactHash = senderName?.let { md5(it) }

        scope.launch {
            val session = repository.getActiveSession() ?: return@launch
            repository.insertEvent(
                NotificationEvent(
                    sessionId = session.sessionId,
                    packageName = sbn.packageName,
                    channelId = sbn.notification?.channelId ?: "default",
                    postTimestamp = sbn.postTime,
                    isInterceptedByDnd = isIntercepted,
                    contactHash = contactHash
                )
            )
        }
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification,
        rankingMap: RankingMap,
        reason: Int
    ) {
        if (NotificationFilter.shouldIgnore(sbn)) return

        val dismissalTime = System.currentTimeMillis()
        val isMassClear = reason == REASON_CANCEL_ALL

        scope.launch {
            val event = repository.getLatestUnresolved(
                sbn.packageName,
                sbn.notification?.channelId ?: "default"
            ) ?: return@launch

            val isGhostOpen = reason == REASON_APP_CANCEL &&
                    ghostOpenDetector.wasOpenedAround(sbn.packageName, dismissalTime)

            repository.updateEvent(
                event.copy(
                    removeTimestamp = dismissalTime,
                    removalReason = reason,
                    isMassClear = isMassClear,
                    isGhostOpen = isGhostOpen
                )
            )
        }
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
