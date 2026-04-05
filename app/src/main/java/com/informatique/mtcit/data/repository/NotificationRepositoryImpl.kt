package com.informatique.mtcit.data.repository

import com.informatique.mtcit.data.api.NotificationApiService
import com.informatique.mtcit.data.model.notification.FcmDeviceTokenReqDto
import com.informatique.mtcit.data.model.notification.NotificationResDto
import com.informatique.mtcit.di.module.RepoServiceState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val api: NotificationApiService,
    private val json: Json
) : NotificationRepository {

    override suspend fun registerDeviceToken(dto: FcmDeviceTokenReqDto): Result<Unit> {
        return when (val state = api.registerDeviceToken(dto)) {
            is RepoServiceState.Success -> Result.success(Unit)
            is RepoServiceState.Error   -> Result.failure(Exception("Register token failed: ${state.error}"))
        }
    }

    override suspend fun unregisterDeviceToken(deviceToken: String): Result<Unit> {
        return when (val state = api.unregisterDeviceToken(deviceToken)) {
            is RepoServiceState.Success -> Result.success(Unit)
            is RepoServiceState.Error   -> Result.failure(Exception("Unregister token failed: ${state.error}"))
        }
    }

    override suspend fun getNotifications(userId: String): Result<List<NotificationResDto>> {
        return when (val state = api.getNotifications(userId)) {
            is RepoServiceState.Success -> {
                try {
                    // ResponseDto<List<NotificationResDto>> wrapper
                    val root = state.response
                    val dataElement = when {
                        root is kotlinx.serialization.json.JsonObject && root.containsKey("data") ->
                            root["data"]
                        else -> root
                    }
                    val list = if (dataElement != null && dataElement !is kotlinx.serialization.json.JsonNull) {
                        json.decodeFromJsonElement<List<NotificationResDto>>(dataElement)
                    } else {
                        emptyList()
                    }
                    Result.success(list)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            is RepoServiceState.Error -> Result.failure(Exception("Load notifications failed: ${state.error}"))
        }
    }

    override suspend fun markAsRead(id: Long): Result<NotificationResDto> {
        return when (val state = api.markAsRead(id)) {
            is RepoServiceState.Success -> {
                try {
                    val root = state.response
                    val dataElement = when {
                        root is kotlinx.serialization.json.JsonObject && root.containsKey("data") ->
                            root["data"]
                        else -> root
                    }
                    val dto = if (dataElement != null && dataElement !is kotlinx.serialization.json.JsonNull) {
                        json.decodeFromJsonElement<NotificationResDto>(dataElement)
                    } else {
                        NotificationResDto(id = id, isRead = 1)
                    }
                    Result.success(dto)
                } catch (e: Exception) {
                    // Return a synthetic read DTO on parse failure
                    Result.success(NotificationResDto(id = id, isRead = 1))
                }
            }
            is RepoServiceState.Error -> Result.failure(Exception("Mark as read failed: ${state.error}"))
        }
    }

    override suspend fun deleteNotification(id: Long): Result<Unit> {
        return when (val state = api.deleteNotification(id)) {
            is RepoServiceState.Success -> Result.success(Unit)
            is RepoServiceState.Error   -> Result.failure(Exception("Delete notification failed: ${state.error}"))
        }
    }

    override suspend fun testSendNotification(eventEnum: String): Result<String> {
        return when (val state = api.testSendNotification(eventEnum)) {
            is RepoServiceState.Success -> {
                // Server returns a plain-text success message; extract it from JSON if needed
                val raw = state.response.toString().trim('"')
                Result.success(raw)
            }
            is RepoServiceState.Error -> Result.failure(Exception("Test failed (${state.code}): ${state.error}"))
        }
    }
}


