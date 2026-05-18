package com.example.notificationauditor.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

object RuleType {
    const val KEYWORD = "KEYWORD"
    const val REGEX = "REGEX"
    const val CONTACT_BLACKLIST = "CONTACT_BLACKLIST"
    const val CONTACT_WHITELIST = "CONTACT_WHITELIST"
}

object RuleAction {
    const val SUPPRESS = "SUPPRESS"
    const val FLAG = "FLAG"
}

@Entity(tableName = "filter_rules")
data class FilterRule(
    @PrimaryKey(autoGenerate = true) val ruleId: Long = 0,
    val ruleType: String,
    val pattern: String,
    val packageName: String? = null,
    val channelId: String? = null,
    val action: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
