package com.example.notificationauditor.util

import android.content.Context

object AppLabelResolver {

    private val appNameCache = HashMap<String, String>()

    fun resolve(context: Context, packageName: String): String {
        return appNameCache.getOrPut(packageName) {
            runCatching {
                context.packageManager
                    .getApplicationLabel(
                        context.packageManager.getApplicationInfo(packageName, 0)
                    )
                    .toString()
            }.getOrDefault(packageName)
        }
    }
}
