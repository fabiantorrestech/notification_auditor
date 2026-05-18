package com.example.notificationauditor.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_insights")
data class DailyInsight(
    // Format: "YYYY-MM-DD" — one row per day, permanent (1-year retention)
    @PrimaryKey
    val date: String,
    val sessionId: Long,
    val totalPosted: Int,
    val clicks: Int,
    val dismissals: Int,
    val ghostOpens: Int,
    val massClearDismissals: Int,
    // JSON array of flagged channels (score < 0.20) serialized as a string
    val recommendationsJson: String = "[]",
    // JSON array of ALL scored channels for the detail view — sorted worst-first
    val channelBreakdownJson: String = "[]"
)
