package com.example.notificationauditor.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.notificationauditor.data.db.entity.DailyInsight

@Dao
interface DailyInsightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(insight: DailyInsight)

    @Query("SELECT * FROM daily_insights ORDER BY date DESC")
    fun observeAll(): LiveData<List<DailyInsight>>

    @Query("SELECT * FROM daily_insights WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    suspend fun getInRange(from: String, to: String): List<DailyInsight>

    @Query("SELECT * FROM daily_insights ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): DailyInsight?

    // Prune insights older than 1 year (called externally with the cutoff date string)
    @Query("SELECT * FROM daily_insights WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DailyInsight?

    @Query("DELETE FROM daily_insights WHERE date < :cutoffDate")
    suspend fun pruneOlderThan(cutoffDate: String)
}
