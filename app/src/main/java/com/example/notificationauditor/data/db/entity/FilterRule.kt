package com.example.notificationauditor.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

object RuleType {
    const val MATCH_ALL = "MATCH_ALL"
    const val KEYWORD = "KEYWORD"
    const val REGEX = "REGEX"
    const val CONTACT_BLACKLIST = "CONTACT_BLACKLIST"
    const val CONTACT_WHITELIST = "CONTACT_WHITELIST"
}

object RuleAction {
    const val EXCLUDE_ONLY = "EXCLUDE_ONLY"
    const val SUPPRESS_EXCLUDE = "SUPPRESS_EXCLUDE"
    const val TAG_ONLY = "TAG_ONLY"

    private const val LEGACY_SUPPRESS = "SUPPRESS"
    private const val LEGACY_FLAG = "FLAG"

    fun normalize(value: String): String = when (value) {
        EXCLUDE_ONLY, SUPPRESS_EXCLUDE, TAG_ONLY -> value
        LEGACY_SUPPRESS -> SUPPRESS_EXCLUDE
        LEGACY_FLAG -> TAG_ONLY
        else -> EXCLUDE_ONLY
    }

    fun excludesFromAnalytics(value: String): Boolean = normalize(value) != TAG_ONLY

    fun suppressesLiveNotification(value: String): Boolean =
        normalize(value) == SUPPRESS_EXCLUDE
}

@Entity(tableName = "filter_rules")
data class FilterRule(
    @PrimaryKey(autoGenerate = true) val ruleId: Long = 0,
    val ruleType: String,
    val pattern: String = "",
    val packageName: String? = null,
    val channelId: String? = null,
    val action: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
