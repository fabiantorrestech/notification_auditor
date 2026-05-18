package com.example.notificationauditor.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.notificationauditor.data.db.entity.FilterRule
import kotlinx.coroutines.flow.Flow

@Dao
interface FilterRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: FilterRule): Long

    @Update
    suspend fun update(rule: FilterRule)

    @Delete
    suspend fun delete(rule: FilterRule)

    @Query("SELECT * FROM filter_rules ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FilterRule>>

    @Query("SELECT * FROM filter_rules WHERE isEnabled = 1 ORDER BY createdAt DESC")
    fun observeEnabled(): Flow<List<FilterRule>>
}
