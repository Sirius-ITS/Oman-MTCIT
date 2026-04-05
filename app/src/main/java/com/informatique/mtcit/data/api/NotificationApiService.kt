package com.informatique.mtcit.data.api

import com.informatique.mtcit.data.model.notification.FcmDeviceTokenReqDto
import com.informatique.mtcit.di.module.AppRepository
import com.informatique.mtcit.di.module.RepoServiceState
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API service for the notifications-ws microservice.
 * Base: https://mtimedevapi.mtcit.gov.om/notifications-ws
 */
@Singleton
class NotificationApiService @Inject constructor(
    private val appRepository: AppRepository,
    private val json: Json
) {
    companion object {
        private const val NOTIFICATIONS_BASE = "https://mtimedevapi.mtcit.gov.om/notifications-ws"
        private const val TOKEN_BASE = "$NOTIFICATIONS_BASE/api/v1/fcm/device-token"
        private const val NOTIF_BASE  = "$NOTIFICATIONS_BASE/api/v1/notifications/fcm"
        // Test endpoint lives on unit-registration-ws (same host, different context root)
        private const val TEST_NOTIF_URL =
            "https://mtimedevapi.mtcit.gov.om/test/api/notification/send-payment-notification"
    }

    /** POST /api/v1/fcm/device-token/register */
    suspend fun registerDeviceToken(dto: FcmDeviceTokenReqDto): RepoServiceState {
        val body = json.encodeToString(dto)
        return appRepository.onPostAuthJson("$TOKEN_BASE/register", body)
    }

    /** DELETE /api/v1/fcm/device-token/unregister/{deviceToken} */
    suspend fun unregisterDeviceToken(deviceToken: String): RepoServiceState {
        return appRepository.onDeleteAuth("$TOKEN_BASE/unregister/$deviceToken")
    }

    /** GET /api/v1/notifications/fcm/user/{userId} */
    suspend fun getNotifications(userId: String): RepoServiceState {
        return appRepository.onGet("$NOTIF_BASE/user/$userId")
    }

    /** PATCH /api/v1/notifications/fcm/{id}/read */
    suspend fun markAsRead(id: Long): RepoServiceState {
        return appRepository.onPatchAuth("$NOTIF_BASE/$id/read", "")
    }

    /** DELETE /api/v1/notifications/fcm/{id} */
    suspend fun deleteNotification(id: Long): RepoServiceState {
        return appRepository.onDeleteAuth("$NOTIF_BASE/$id")
    }

    /**
     * POST /test/api/notification/send-payment-notification
     * Test endpoint on unit-registration-ws – triggers FCM push end-to-end.
     * eventEnum: PAYMENT_SUCCESS | PAYMENT_FAILURE | PAYMENT_ABORTED
     */
    suspend fun testSendNotification(eventEnum: String): RepoServiceState {
        val body = """{"eventEnum":"$eventEnum"}"""
        return appRepository.onPostAuthJson(TEST_NOTIF_URL, body)
    }
}



