package com.informatique.mtcit.data.repository

import com.informatique.mtcit.data.model.notification.FcmDeviceTokenReqDto
import com.informatique.mtcit.data.model.notification.NotificationResDto

interface NotificationRepository {
    suspend fun registerDeviceToken(dto: FcmDeviceTokenReqDto): Result<Unit>
    suspend fun unregisterDeviceToken(deviceToken: String): Result<Unit>
    suspend fun getNotifications(userId: String): Result<List<NotificationResDto>>
    suspend fun markAsRead(id: Long): Result<NotificationResDto>
    suspend fun deleteNotification(id: Long): Result<Unit>
    /** Fires a test push via unit-registration-ws. Returns the plain-text response message. */
    suspend fun testSendNotification(eventEnum: String): Result<String>
}


