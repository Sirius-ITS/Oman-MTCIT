package com.informatique.mtcit.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.informatique.mtcit.data.datastorehelper.TokenManager
import com.informatique.mtcit.data.model.notification.FcmDeviceTokenReqDto
import com.informatique.mtcit.data.model.notification.NotificationResDto
import com.informatique.mtcit.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository,
    @ApplicationContext private val appContext: Context
) : BaseViewModel() {

    companion object {
        private const val TAG = "NotificationViewModel"
    }

    private val _notifications = MutableStateFlow<List<NotificationResDto>>(emptyList())
    val notifications: StateFlow<List<NotificationResDto>> = _notifications.asStateFlow()

    /** Number of unread (isRead == 0) notifications */
    val unreadCount: StateFlow<Int> get() = _unreadCount
    private val _unreadCount = MutableStateFlow(0)

    /** Whether the user has notifications turned on (mirrors the Settings switch). */
    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    // -----------------------------------------------------------------------
    // Notifications enabled preference
    // -----------------------------------------------------------------------

    fun loadNotificationsEnabled() {
        viewModelScope.launch {
            _notificationsEnabled.value = TokenManager.isNotificationsEnabled(appContext)
        }
    }

    /**
     * Toggle notifications on/off.
     * ON  → fetches FCM token, registers with backend, saves state.
     * OFF → unregisters from backend, saves state.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            setLoading(true)
            val userId = TokenManager.getCivilId(appContext)
            if (userId.isNullOrBlank()) {
                Log.w(TAG, "⚠️ Cannot toggle notifications: userId is null")
                setLoading(false)
                return@launch
            }

            val fcmToken = getOrFetchFcmToken()
            if (fcmToken.isNullOrBlank()) {
                Log.e(TAG, "❌ Cannot toggle notifications: FCM token is null")
                setLoading(false)
                return@launch
            }

            if (enabled) {
                val dto = FcmDeviceTokenReqDto(deviceToken = fcmToken, userId = userId, deviceType = "ANDROID")
                repository.registerDeviceToken(dto)
                    .onSuccess {
                        TokenManager.saveNotificationsEnabled(appContext, true)
                        TokenManager.saveFcmRegistered(appContext, true)
                        _notificationsEnabled.value = true
                        Log.d(TAG, "✅ Notifications enabled & FCM registered")
                    }
                    .onFailure { e -> Log.e(TAG, "❌ Enable notifications failed: ${e.message}") }
            } else {
                repository.unregisterDeviceToken(fcmToken)
                    .onSuccess {
                        TokenManager.saveNotificationsEnabled(appContext, false)
                        TokenManager.saveFcmRegistered(appContext, false)
                        _notificationsEnabled.value = false
                        Log.d(TAG, "✅ Notifications disabled & FCM unregistered")
                    }
                    .onFailure { e -> Log.e(TAG, "❌ Disable notifications failed: ${e.message}") }
            }
            setLoading(false)
        }
    }

    /** Returns the stored FCM token, fetching from Firebase if not yet cached. */
    private suspend fun getOrFetchFcmToken(): String? {
        var token = TokenManager.getFcmToken(appContext)
        if (token.isNullOrBlank()) {
            token = suspendCancellableCoroutine { cont ->
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { cont.resumeWith(Result.success(it)) }
                    .addOnFailureListener { cont.resumeWith(Result.success(null)) }
            }
            token?.let { TokenManager.saveFcmToken(appContext, it) }
        }
        return token
    }



    fun loadNotifications(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            setLoading(true)
            repository.getNotifications(userId)
                .onSuccess { list ->
                    _notifications.value = list
                    _unreadCount.value = list.count { it.isRead == 0 }
                    Log.d(TAG, "✅ Loaded ${list.size} notifications, unread: ${_unreadCount.value}")
                }
                .onFailure { e ->
                    Log.e(TAG, "❌ Load notifications failed: ${e.message}")
                    // Don't show error to user – badge just stays at 0
                }
            setLoading(false)
        }
    }

    // -----------------------------------------------------------------------
    // Mark as read  (optimistic update + API)
    // -----------------------------------------------------------------------

    fun markAsRead(id: Long) {
        viewModelScope.launch {
            // Optimistic UI update
            _notifications.value = _notifications.value.map { n ->
                if (n.id == id && n.isRead == 0) n.copy(isRead = 1) else n
            }
            _unreadCount.value = _notifications.value.count { it.isRead == 0 }

            repository.markAsRead(id)
                .onFailure { e ->
                    Log.e(TAG, "❌ Mark as read failed for id=$id: ${e.message}")
                    // Revert optimistic update on failure
                    _notifications.value = _notifications.value.map { n ->
                        if (n.id == id) n.copy(isRead = 0) else n
                    }
                    _unreadCount.value = _notifications.value.count { it.isRead == 0 }
                }
        }
    }

    // -----------------------------------------------------------------------
    // Delete  (optimistic update + API)
    // -----------------------------------------------------------------------

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            val removed = _notifications.value.find { it.id == id }
            // Optimistic remove
            _notifications.value = _notifications.value.filter { it.id != id }
            _unreadCount.value = _notifications.value.count { it.isRead == 0 }

            repository.deleteNotification(id)
                .onFailure { e ->
                    Log.e(TAG, "❌ Delete notification failed for id=$id: ${e.message}")
                    // Revert: put back at original position
                    removed?.let {
                        _notifications.value = (_notifications.value + it)
                            .sortedByDescending { n -> n.id }
                        _unreadCount.value = _notifications.value.count { n -> n.isRead == 0 }
                    }
                }
        }
    }

    // -----------------------------------------------------------------------
    // FCM token registration / unregistration
    // -----------------------------------------------------------------------

    fun registerFcmToken(userId: String, deviceToken: String) {
        if (userId.isBlank() || deviceToken.isBlank()) return
        viewModelScope.launch {
            val dto = FcmDeviceTokenReqDto(
                deviceToken = deviceToken,
                userId = userId,
                deviceType = "ANDROID"
            )
            repository.registerDeviceToken(dto)
                .onSuccess {
                    Log.d(TAG, "✅ FCM token registered for user=$userId")
                    TokenManager.saveFcmRegistered(appContext, true)
                    TokenManager.saveNotificationsEnabled(appContext, true)
                    _notificationsEnabled.value = true
                }
                .onFailure { e -> Log.e(TAG, "❌ FCM token registration failed: ${e.message}") }
        }
    }

    fun unregisterFcmToken(deviceToken: String) {
        if (deviceToken.isBlank()) return
        viewModelScope.launch {
            repository.unregisterDeviceToken(deviceToken)
                .onSuccess {
                    Log.d(TAG, "✅ FCM token unregistered")
                    TokenManager.saveFcmRegistered(appContext, false)
                }
                .onFailure { e -> Log.e(TAG, "❌ FCM token unregister failed: ${e.message}") }
        }
    }

}






