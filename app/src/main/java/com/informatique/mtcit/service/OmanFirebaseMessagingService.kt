package com.informatique.mtcit.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.informatique.mtcit.R
import com.informatique.mtcit.data.datastorehelper.TokenManager
import com.informatique.mtcit.ui.LandingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Firebase Cloud Messaging Service.
 * - Saves the FCM device token to DataStore on refresh.
 * - Shows a local notification when a message arrives in the foreground.
 */
class OmanFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "OmanFCMService"
        const val CHANNEL_ID = "oman_notifications"
        const val CHANNEL_NAME = "Oman Notifications"
    }

    /** Called whenever Firebase issues a new device token. */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔑 New FCM token: $token")

        // Persist the token so NavHost can register it after login
        CoroutineScope(Dispatchers.IO).launch {
            TokenManager.saveFcmToken(applicationContext, token)
        }
    }

    /** Called when a push message arrives while the app is in the foreground. */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "📩 Message received from: ${message.from}")

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "إشعار جديد"
        val body  = message.notification?.body
            ?: message.data["body"]
            ?: ""

        showNotification(title, body)
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel (required on Android O+)
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)

        val intent = Intent(this, LandingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

