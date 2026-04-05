package com.informatique.mtcit.data.model.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request DTO for registering an FCM device token
 */
@Serializable
data class FcmDeviceTokenReqDto(
    @SerialName("deviceToken") val deviceToken: String,
    @SerialName("userId") val userId: String,
    @SerialName("deviceType") val deviceType: String = "ANDROID",
    @SerialName("deviceId") val deviceId: String = "",
    @SerialName("isActive") val isActive: Int = 1
)

/**
 * Response DTO for a single user notification
 */
@Serializable
data class NotificationResDto(
    @SerialName("id") val id: Long,
    @SerialName("userId") val userId: String = "",
    @SerialName("title") val title: String? = null,
    @SerialName("body") val body: String? = null,
    @SerialName("isRead") val isRead: Int = 0
)

/**
 * Generic API response wrapper used by notifications-ws
 */
@Serializable
data class NotificationResponseDto<T>(
    @SerialName("data") val data: T? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("success") val success: Boolean = false
)

