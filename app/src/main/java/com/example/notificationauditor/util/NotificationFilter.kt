package com.example.notificationauditor.util

import android.app.Notification
import android.service.notification.StatusBarNotification

object NotificationFilter {

    private val excludedCategories = setOf(
        Notification.CATEGORY_TRANSPORT,
        Notification.CATEGORY_NAVIGATION,
        Notification.CATEGORY_SERVICE
    )

    fun shouldIgnore(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification ?: return true
        val flags = notification.flags

        // Discard persistent service/ongoing notifications the user can't swipe away
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) return true
        if (flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return true
        if (flags and Notification.FLAG_NO_CLEAR != 0) return true

        // Discard the OS-generated group summary — only log the actual content nodes
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return true

        // Discard media player controls, navigation bars, and background service alerts
        if (notification.category in excludedCategories) return true

        return false
    }

    // Extracts a sender name from MessagingStyle extras; returns null for non-messaging notifications
    fun extractSenderName(sbn: StatusBarNotification): String? {
        val extras = sbn.notification?.extras ?: return null

        // Modern MessagingStyle provides a Person object
        val messagingPerson = extras.getParcelable<android.app.Person>(Notification.EXTRA_MESSAGING_PERSON)
        if (messagingPerson?.name != null) return messagingPerson.name.toString()

        // Fallback: first message in EXTRA_MESSAGES array
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (!messages.isNullOrEmpty()) {
            val first = messages[0]
            if (first is android.os.Bundle) {
                val sender = first.getCharSequence("sender")
                if (sender != null) return sender.toString()
            }
        }

        return null
    }
}
