package com.informatique.educationComposeVersion.common

import com.informatique.educationComposeVersion.BuildConfig

object Const {
    const val API_KEY = BuildConfig.API_KEY
    //{"UserName":" 2420302","Password":"Sue@68193"}
    const val BASE_URL = "http://192.168.1.60:3002/"
    const val DB_NAME = "article_db"
    //WorkManager and Notification
    const val MORNING_UPDATE_TIME = 5
    const val NOTIFICATION_ID = 1
    const val NOTIFICATION_CHANNEL_ID = "skeleton_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Skeleton Channel"
    const val NOTIFICATION_CONTENT_TITLE = "Skeleton App Update"
    const val NOTIFICATION_CONTENT_TEXT = "Check out the latest ..."

}