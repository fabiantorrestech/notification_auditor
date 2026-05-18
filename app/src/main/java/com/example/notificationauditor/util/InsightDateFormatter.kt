package com.example.notificationauditor.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object InsightDateFormatter {

    private val parser = DateTimeFormatter.ISO_LOCAL_DATE
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.US)
    private val weekdayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.US)

    fun format(rawDate: String): String {
        val date = runCatching { LocalDate.parse(rawDate, parser) }.getOrNull() ?: return rawDate
        val month = date.format(monthFormatter)
        val weekday = date.format(weekdayFormatter)
        return "$month ${date.dayOfMonth}${ordinalSuffix(date.dayOfMonth)}, ${date.year} - $weekday"
    }

    private fun ordinalSuffix(day: Int): String {
        if (day in 11..13) return "th"
        return when (day % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
}
