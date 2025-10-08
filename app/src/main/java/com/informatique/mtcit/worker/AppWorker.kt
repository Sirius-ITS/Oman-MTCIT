package com.informatique.mtcit.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.informatique.mtcit.R
import com.informatique.mtcit.common.Const
import com.informatique.mtcit.common.Const.NOTIFICATION_ID
// import com.informatique.mtcit.data.network.ApiInterface
import com.informatique.mtcit.ui.LandingActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AppWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParameters: WorkerParameters,
    //private val network: ApiInterface
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        lateinit var result: Result
        kotlin.runCatching {
        }.onSuccess {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                sendNotification()
            }
            result = Result.success()
        }.onFailure {
            result = Result.retry()
        }
        return result
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendNotification() {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a notification channel
        val channel = NotificationChannel(
            Const.NOTIFICATION_CHANNEL_ID,
            Const.NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(applicationContext, LandingActivity::class.java)
        intent.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra("notification_id", NOTIFICATION_ID)

        // Create a PendingIntent
        val pendingIntent = getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification =
            NotificationCompat.Builder(applicationContext, Const.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentText(Const.NOTIFICATION_CONTENT_TEXT)
                .setContentTitle(Const.NOTIFICATION_CONTENT_TITLE)
                .setContentIntent(pendingIntent)
                .build()

        // Show the notification
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

}