package com.example.notificationauditor.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.notificationauditor.data.db.entity.StudySession

@Dao
interface StudySessionDao {

    @Insert
    suspend fun insert(session: StudySession): Long

    @Update
    suspend fun update(session: StudySession)

    @Query("SELECT * FROM study_sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSession(): StudySession?

    @Query("SELECT * FROM study_sessions WHERE isActive = 1 LIMIT 1")
    fun observeActiveSession(): LiveData<StudySession?>

    @Query("UPDATE study_sessions SET isActive = 0, endTimestamp = :endTs WHERE isActive = 1")
    suspend fun deactivateAll(endTs: Long = System.currentTimeMillis())
}
