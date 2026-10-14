package com.example.jarvis

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Stores only the latest notification locally. Nothing is sent over the network.
 * Android requires the user to enable notification access manually.
 */
class JarvisNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val body = extras.getCharSequence("android.text")?.toString().orEmpty()
        if (title.isBlank() && body.isBlank()) return
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LATEST, listOf(title, body).filter { it.isNotBlank() }.joinToString(". "))
            .apply()
    }

    companion object {
        private const val PREFS = "jarvis_local_notifications"
        private const val KEY_LATEST = "latest_notification"
        fun latest(context: Context): String =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LATEST, "").orEmpty()
    }
}
