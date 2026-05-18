package com.example.notificationauditor.service

import android.os.Process
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.notificationauditor.data.db.AppDatabase
import com.example.notificationauditor.data.db.entity.NotificationEvent
import com.example.notificationauditor.data.db.entity.RuleAction
import com.example.notificationauditor.data.repository.NotificationRepository
import com.example.notificationauditor.util.DndTracker
import com.example.notificationauditor.util.FilterEngine
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
    private lateinit var filterEngine: FilterEngine

    override fun onCreate() {
        super.onCreate()
        instance = this
        val db = AppDatabase.getInstance(applicationContext)
        repository = NotificationRepository(db)
        dndTracker = DndTracker(applicationContext)
        ghostOpenDetector = GhostOpenDetector(applicationContext)
        filterEngine = FilterEngine()
        scope.launch {
            repository.observeEnabledRules().collect { rules ->
                filterEngine.updateRules(rules)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
        job.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        if (NotificationFilter.shouldIgnore(sbn)) return

        val extras = sbn.notification?.extras
        val title = extras?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()
        val body = extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
        val senderName = NotificationFilter.extractSenderName(sbn)
        val contactHash = senderName?.let { md5(it) }
        val channelId = sbn.notification?.channelId ?: "default"

        val filterResult = filterEngine.evaluate(
            packageName = sbn.packageName,
            channelId = channelId,
            title = title,
            body = body,
            contactHash = contactHash
        )

        if (filterResult != null) {
            scope.launch {
                val session = repository.getActiveSession() ?: return@launch
                repository.insertEvent(
                    NotificationEvent(
                        sessionId = session.sessionId,
                        packageName = sbn.packageName,
                        channelId = channelId,
                        postTimestamp = sbn.postTime,
                        contactHash = contactHash,
                        filteredByRuleId = filterResult.ruleId,
                        filterEffect = filterResult.effect,
                        excludeFromAnalytics = filterResult.excludeFromAnalytics
                    )
                )
                if (RuleAction.suppressesLiveNotification(filterResult.effect)) {
                    cancelNotification(sbn.key)
                }
            }
            return
        }

        val isIntercepted = dndTracker.isInterceptedByDnd(sbn.key, rankingMap)

        scope.launch {
            val session = repository.getActiveSession() ?: return@launch
            repository.insertEvent(
                NotificationEvent(
                    sessionId = session.sessionId,
                    packageName = sbn.packageName,
                    channelId = channelId,
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

    companion object {
        private const val FULL_CHANNEL_LOOKUP_UNAVAILABLE =
            "Full channel lookup requires a connected notification listener."

        @Volatile
        private var instance: NotificationHarvesterService? = null

        fun getActiveChannelIdsForPackage(packageName: String): List<String> {
            val service = instance ?: return emptyList()
            return runCatching {
                service.activeNotifications
                    .asSequence()
                    .filter { it.packageName == packageName }
                    .mapNotNull { it.notification?.channelId }
                    .distinct()
                    .sortedBy { it.lowercase() }
                    .toList()
            }.getOrDefault(emptyList())
        }

        fun getNotificationChannelsForPackage(packageName: String): AppChannelLookupResult {
            val service = instance ?: return AppChannelLookupResult(
                isAvailable = false,
                unavailableMessage = FULL_CHANNEL_LOOKUP_UNAVAILABLE
            )

            return runCatching {
                AppChannelLookupResult(
                    channels = service.getNotificationChannels(packageName, Process.myUserHandle())
                        .asSequence()
                        .mapNotNull { channel ->
                            val channelId = channel.id.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            AppNotificationChannel(
                                channelId = channelId,
                                channelName = channel.name?.toString()?.takeIf { it.isNotBlank() }
                            )
                        }
                        .sortedBy { channel ->
                            (channel.channelName ?: channel.channelId).lowercase()
                        }
                        .toList(),
                    isAvailable = true
                )
            }.getOrElse {
                AppChannelLookupResult(
                    isAvailable = false,
                    unavailableMessage = "Couldn't load channels from the connected notification listener."
                )
            }
        }
    }
}

data class AppNotificationChannel(
    val channelId: String,
    val channelName: String?
)

data class AppChannelLookupResult(
    val channels: List<AppNotificationChannel> = emptyList(),
    val isAvailable: Boolean,
    val unavailableMessage: String? = null
)
