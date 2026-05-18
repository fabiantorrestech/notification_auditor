package com.example.notificationauditor.util

import android.content.Context
import android.text.format.DateFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DailyCutoff(
    val hour: Int,
    val minute: Int
)

data class CompletedCutoffWindow(
    val labelDate: String,
    val startEpochMs: Long,
    val endEpochMs: Long
)

object DailyCutoffTime {

    private val labelFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    fun load(context: Context): DailyCutoff {
        val prefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
        return DailyCutoff(
            hour = prefs.getInt(AppPrefs.KEY_CUTOFF_HOUR, AppPrefs.DEFAULT_CUTOFF_HOUR),
            minute = prefs.getInt(AppPrefs.KEY_CUTOFF_MINUTE, AppPrefs.DEFAULT_CUTOFF_MINUTE)
        )
    }

    fun save(context: Context, cutoff: DailyCutoff) {
        context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(AppPrefs.KEY_CUTOFF_HOUR, cutoff.hour)
            .putInt(AppPrefs.KEY_CUTOFF_MINUTE, cutoff.minute)
            .apply()
    }

    fun format(context: Context, cutoff: DailyCutoff): String {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        return LocalTime.of(cutoff.hour, cutoff.minute)
            .format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }

    fun nextDelayMillis(
        nowMs: Long,
        cutoff: DailyCutoff,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val now = Instant.ofEpochMilli(nowMs).atZone(zoneId)
        val nextCutoff = nextCutoffAfter(now.toLocalDateTime(), cutoff)
            .atZone(zoneId)
        return Duration.between(now, nextCutoff).toMillis().coerceAtLeast(0L)
    }

    fun completedWindow(
        nowMs: Long,
        cutoff: DailyCutoff,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): CompletedCutoffWindow {
        val now = Instant.ofEpochMilli(nowMs).atZone(zoneId).toLocalDateTime()
        val windowEnd = mostRecentCompletedCutoff(now, cutoff)
        val windowStart = windowEnd.minusDays(1)
        return CompletedCutoffWindow(
            labelDate = windowEnd.toLocalDate().format(labelFormatter),
            startEpochMs = windowStart.atZone(zoneId).toInstant().toEpochMilli(),
            endEpochMs = windowEnd.atZone(zoneId).toInstant().toEpochMilli()
        )
    }

    private fun nextCutoffAfter(now: LocalDateTime, cutoff: DailyCutoff): LocalDateTime {
        val cutoffToday = atCutoff(now.toLocalDate(), cutoff)
        return if (now.isBefore(cutoffToday)) cutoffToday else cutoffToday.plusDays(1)
    }

    private fun mostRecentCompletedCutoff(now: LocalDateTime, cutoff: DailyCutoff): LocalDateTime {
        val cutoffToday = atCutoff(now.toLocalDate(), cutoff)
        return if (now.isBefore(cutoffToday)) cutoffToday.minusDays(1) else cutoffToday
    }

    private fun atCutoff(date: LocalDate, cutoff: DailyCutoff): LocalDateTime =
        LocalDateTime.of(date, LocalTime.of(cutoff.hour, cutoff.minute))
}
