package com.example.notificationauditor.util

import android.content.Context
import android.graphics.drawable.Drawable

object AppIconResolver {

    private val appIconStateCache = HashMap<String, Drawable.ConstantState?>()

    fun resolve(context: Context, packageName: String): Drawable {
        val defaultIcon = context.packageManager.defaultActivityIcon
        val constantState = appIconStateCache.getOrPut(packageName) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName).constantState
            }.getOrNull()
        }
        return constantState?.newDrawable(context.resources)?.mutate() ?: defaultIcon
    }
}
