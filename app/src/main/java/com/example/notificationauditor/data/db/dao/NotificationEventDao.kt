package com.example.notificationauditor.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.notificationauditor.data.db.entity.NotificationEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationEventDao {

    @Insert
    suspend fun insert(event: NotificationEvent): Long

    @Update
    suspend fun update(event: NotificationEvent)

    @Query("SELECT * FROM notification_events WHERE eventId = :eventId")
    suspend fun getById(eventId: Long): NotificationEvent?

    // Looks up the most recent unresolved posted event for a given package + channel
    @Query("""
        SELECT * FROM notification_events
        WHERE packageName = :packageName AND channelId = :channelId AND removeTimestamp IS NULL
        ORDER BY postTimestamp DESC LIMIT 1
    """)
    suspend fun getLatestUnresolved(packageName: String, channelId: String): NotificationEvent?

    @Query("SELECT * FROM notification_events WHERE sessionId = :sessionId AND postTimestamp BETWEEN :from AND :to")
    suspend fun getEventsInWindow(sessionId: Long, from: Long, to: Long): List<NotificationEvent>

    @Query("SELECT * FROM notification_events WHERE sessionId = :sessionId AND packageName = :pkg AND channelId = :channelId AND postTimestamp BETWEEN :from AND :to")
    suspend fun getChannelEventsInWindow(sessionId: Long, pkg: String, channelId: String, from: Long, to: Long): List<NotificationEvent>

    @Query("SELECT COUNT(*) FROM notification_events WHERE sessionId = :sessionId")
    suspend fun countForSession(sessionId: Long): Int

    @Query("DELETE FROM notification_events WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)

    @Query("DELETE FROM notification_events WHERE postTimestamp < :cutoff")
    suspend fun pruneEventsBefore(cutoff: Long)

    @Query("SELECT * FROM notification_events WHERE filteredByRuleId IS NOT NULL ORDER BY postTimestamp DESC")
    fun observeFiltered(): Flow<List<NotificationEvent>>

    // Returns distinct (packageName, channelId) pairs active in the given window
    @Query("""
        SELECT DISTINCT packageName, channelId FROM notification_events
        WHERE sessionId = :sessionId AND postTimestamp BETWEEN :from AND :to
    """)
    suspend fun getActiveChannels(sessionId: Long, from: Long, to: Long): List<ChannelKey>
}

data class ChannelKey(val packageName: String, val channelId: String)
