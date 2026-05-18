package com.example.notificationauditor.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_events",
    foreignKeys = [
        ForeignKey(
            entity = StudySession::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("packageName"), Index("channelId"), Index("postTimestamp"), Index("filteredByRuleId")]
)
data class NotificationEvent(
    @PrimaryKey(autoGenerate = true)
    val eventId: Long = 0,
    val sessionId: Long,
    val packageName: String,
    val channelId: String,
    val postTimestamp: Long,
    val removeTimestamp: Long? = null,
    // Maps to NotificationListenerService reason constants
    val removalReason: Int? = null,
    val isInterceptedByDnd: Boolean = false,
    // True when dismissed as part of a REASON_CANCEL_ALL batch
    val isMassClear: Boolean = false,
    // True when REASON_APP_CANCEL + app foregrounded within window
    val isGhostOpen: Boolean = false,
    // MD5 of sender name from MessagingStyle (nullable — not all notifications have a sender)
    val contactHash: String? = null,
    val filteredByRuleId: Long? = null
)
