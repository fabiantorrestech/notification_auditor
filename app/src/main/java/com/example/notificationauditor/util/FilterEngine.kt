package com.example.notificationauditor.util

import com.example.notificationauditor.data.db.entity.FilterRule
import com.example.notificationauditor.data.db.entity.RuleAction
import com.example.notificationauditor.data.db.entity.RuleType

data class FilterResult(val ruleId: Long, val action: String)

class FilterEngine {

    @Volatile private var rules: List<FilterRule> = emptyList()
    @Volatile private var compiledRegexes: Map<Long, Regex> = emptyMap()

    fun updateRules(newRules: List<FilterRule>) {
        compiledRegexes = newRules
            .filter { it.ruleType == RuleType.REGEX }
            .mapNotNull { rule ->
                runCatching { rule.ruleId to Regex(rule.pattern, RegexOption.IGNORE_CASE) }
                    .getOrNull()
            }
            .toMap()
        rules = newRules
    }

    fun evaluate(
        packageName: String,
        channelId: String,
        title: String?,
        body: String?,
        contactHash: String?
    ): FilterResult? {
        val snapshot = rules

        // Scope: keep rules applicable to this package + channel
        val applicable = snapshot.filter { rule ->
            val pkgMatch = rule.packageName == null || rule.packageName == packageName
            val chMatch = rule.channelId == null ||
                    (rule.packageName != null && rule.channelId == channelId)
            pkgMatch && chMatch
        }

        // CONTACT_WHITELIST — evaluated as a group before per-rule loop
        val whitelistRules = applicable.filter { it.ruleType == RuleType.CONTACT_WHITELIST }
        if (whitelistRules.isNotEmpty()) {
            val allowed = contactHash != null && whitelistRules.any { it.pattern == contactHash }
            if (!allowed) {
                val rep = whitelistRules.first()
                return FilterResult(rep.ruleId, rep.action)
            }
        }

        // Per-rule loop — first match wins
        val text = buildString {
            title?.let { append(it) }
            if (title != null && body != null) append(" ")
            body?.let { append(it) }
        }

        for (rule in applicable) {
            when (rule.ruleType) {
                RuleType.KEYWORD -> {
                    if (text.contains(rule.pattern, ignoreCase = true)) {
                        return FilterResult(rule.ruleId, rule.action)
                    }
                }
                RuleType.REGEX -> {
                    val regex = compiledRegexes[rule.ruleId] ?: continue
                    if (regex.containsMatchIn(text)) {
                        return FilterResult(rule.ruleId, rule.action)
                    }
                }
                RuleType.CONTACT_BLACKLIST -> {
                    if (contactHash != null && contactHash == rule.pattern) {
                        return FilterResult(rule.ruleId, rule.action)
                    }
                }
                RuleType.CONTACT_WHITELIST -> continue
            }
        }

        return null
    }
}
