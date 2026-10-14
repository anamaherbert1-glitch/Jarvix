package com.example.jarvis

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Notifications accessibles seulement après activation manuelle par l’utilisateur. */
class JarvisNotificationListener : NotificationListenerService() {
 override fun onNotificationPosted(sbn: StatusBarNotification?) {
  // Prototype : aucune collecte, lecture automatique ou transmission réseau.
 }
}
