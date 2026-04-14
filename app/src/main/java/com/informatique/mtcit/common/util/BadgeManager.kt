package com.informatique.mtcit.common.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.informatique.mtcit.R

/**
 * Drives the launcher icon badge count using a silent system notification.
 *
 * Works on:
 *  • Samsung / One UI   → shows the actual number on the icon
 *  • Xiaomi / MIUI      → shows the actual number
 *  • Huawei, Oppo, Vivo → shows the actual number on most launchers
 *  • Stock Android (Pixel) → shows a dot (Android 8+ default)
 *
 * No external library required — minSdk 26 means NotificationChannel is always available.
 */
object BadgeManager {

    private const val BADGE_CHANNEL_ID      = "badge_count_channel"
    private const val BADGE_NOTIFICATION_ID = 8888

    /**
     * Update the app icon badge.
     * @param count Pass 0 or negative to clear the badge entirely.
     */
    fun update(context: Context, count: Int) {
        ensureChannel(context)

        val nm = NotificationManagerCompat.from(context)

        if (count <= 0) {
            nm.cancel(BADGE_NOTIFICATION_ID)
            return
        }

        // Android 13+ requires POST_NOTIFICATIONS permission at runtime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        // Tapping the badge notification opens the app
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = if (count == 1) "1 unread notification" else "$count unread notifications"

        val notification = NotificationCompat.Builder(context, BADGE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_splash_logo)   // monochrome app icon
            .setContentTitle("MTCIT Oman")
            .setContentText(body)
            .setNumber(count)           // ← this is what drives the badge number
            .setContentIntent(pendingIntent)
            .setSilent(true)            // no sound or vibration
            .setOnlyAlertOnce(true)     // don't re-alert on count updates
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        nm.notify(BADGE_NOTIFICATION_ID, notification)
    }

    // ── Channel setup (called once; safe to call repeatedly) ─────────────────
    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            BADGE_CHANNEL_ID,
            "Badge Counter",
            NotificationManager.IMPORTANCE_LOW   // quiet — no heads-up, still badges
        ).apply {
            description = "Shows unread notification count on the app icon"
            setShowBadge(true)
            setSound(null, null)
            enableVibration(false)
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}

