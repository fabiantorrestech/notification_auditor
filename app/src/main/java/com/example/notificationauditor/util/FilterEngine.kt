package com.example.notificationauditor.util

import com.example.notificationauditor.data.db.entity.FilterRule
import com.example.notificationauditor.data.db.entity.RuleAction
import com.example.notificationauditor.data.db.entity.RuleType

data class FilterResult(
    val ruleId: Long,
    val effect: String,
    val excludeFromAnalytics: Boolean
)

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
        val text = buildString {
            title?.let { append(it) }
            if (title != null && body != null) append(" ")
            body?.let { append(it) }
        }

        val snapshot = rules.sortedWith(
            compareByDescending<FilterRule> { it.createdAt }
                .thenByDescending { it.ruleId }
        )

        val scopedRules = listOf(
            snapshot.filter { it.packageName == packageName && it.channelId == channelId },
            snapshot.filter { it.packageName == packageName && it.channelId == null },
            snapshot.filter { it.packageName == null && it.channelId == null }
        )

        for (scopeRules in scopedRules) {
            if (scopeRules.isEmpty()) continue

            val whitelistRules = scopeRules.filter { it.ruleType == RuleType.CONTACT_WHITELIST }
            if (whitelistRules.isNotEmpty()) {
                val allowed = contactHash != null && whitelistRules.any { it.pattern == contactHash }
                if (!allowed) {
                    return whitelistRules.first().toResult()
                }
            }

            for (rule in scopeRules) {
                when (rule.ruleType) {
                    RuleType.MATCH_ALL -> continue
                    RuleType.KEYWORD -> {
                        if (text.contains(rule.pattern, ignoreCase = true)) {
                            return rule.toResult()
                        }
                    }
                    RuleType.REGEX -> {
                        val regex = compiledRegexes[rule.ruleId] ?: continue
                        if (regex.containsMatchIn(text)) {
                            return rule.toResult()
                        }
                    }
                    RuleType.CONTACT_BLACKLIST -> {
                        if (contactHash != null && contactHash == rule.pattern) {
                            return rule.toResult()
                        }
                    }
                    RuleType.CONTACT_WHITELIST -> continue
                }
            }

            scopeRules.firstOrNull { it.ruleType == RuleType.MATCH_ALL }?.let { rule ->
                return rule.toResult()
            }
        }

        return null
    }

    private fun FilterRule.toResult(): FilterResult {
        val effect = RuleAction.normalize(action)
        return FilterResult(
            ruleId = ruleId,
            effect = effect,
            excludeFromAnalytics = RuleAction.excludesFromAnalytics(effect)
        )
    }
}
