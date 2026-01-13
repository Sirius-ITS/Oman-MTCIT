package com.informatique.mtcit.ui.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.informatique.mtcit.business.requests.RequestsStrategy
import com.informatique.mtcit.common.ApiException
import com.informatique.mtcit.common.AppError
import com.informatique.mtcit.data.api.RequestsApiService
import com.informatique.mtcit.data.model.requests.PaginationState
import com.informatique.mtcit.data.model.requests.StatusCountData
import com.informatique.mtcit.data.model.requests.UserRequestUiModel
import com.informatique.mtcit.data.repository.AuthRepository
import com.informatique.mtcit.util.UserHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for User Requests
 * Handles UI state, pagination, and data loading with error handling
 */
@HiltViewModel
class RequestsViewModel @Inject constructor(
    private val requestsStrategy: RequestsStrategy,
    private val requestsApiService: RequestsApiService, // ✅ NEW: Inject RequestsApiService
    private val authRepository: AuthRepository, // ✅ NEW: Inject AuthRepository for token refresh
    @ApplicationContext private val appContext: Context
) : BaseViewModel() {

    private val _requests = MutableStateFlow<List<UserRequestUiModel>>(emptyList())
    val requests: StateFlow<List<UserRequestUiModel>> = _requests.asStateFlow()

    private val _paginationState = MutableStateFlow(PaginationState())
    val paginationState: StateFlow<PaginationState> = _paginationState.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // ✅ Override error state with AppError type (instead of String?)
    private val _appError = MutableStateFlow<AppError?>(null)
    val appError: StateFlow<AppError?> = _appError.asStateFlow()

    // ✅ NEW: Navigation trigger for request detail
    private val _navigationToRequestDetail = MutableStateFlow<Pair<Int, Int>?>(null)
    val navigationToRequestDetail: StateFlow<Pair<Int, Int>?> = _navigationToRequestDetail.asStateFlow()

    // ✅ NEW: Navigation to login trigger (like MainCategoriesViewModel)
    private val _shouldNavigateToLogin = MutableStateFlow(false)
    val shouldNavigateToLogin: StateFlow<Boolean> = _shouldNavigateToLogin.asStateFlow()

    // ✅ NEW: Status counts state
    private val _statusCountData = MutableStateFlow<StatusCountData?>(null)
    val statusCountData: StateFlow<StatusCountData?> = _statusCountData.asStateFlow()

    private val _isLoadingStatusCounts = MutableStateFlow(false)
    val isLoadingStatusCounts: StateFlow<Boolean> = _isLoadingStatusCounts.asStateFlow()

    private var currentCivilId: String? = null
    private val pageSize = 10

    /**
     * Load first page of requests
     */
    fun loadRequests() {
        viewModelScope.launch {
            try {
                setLoading(true)
                _appError.value = null

                val civilId = UserHelper.getOwnerCivilId(appContext)

                // ✅ NEW: Check if civil ID is null (no token available)
                if (civilId == null) {
                    println("⚠️ RequestsViewModel: No token found, triggering login flow")
                    setLoading(false)
                    _appError.value = AppError.Unauthorized("لم يتم العثور على رمز الدخول. الرجاء تسجيل الدخول للمتابعة")
                    return@launch
                }

                currentCivilId = civilId

                println("📱 RequestsViewModel: Loading first page for civilId=$civilId")

                val result = requestsStrategy.loadUserRequests(
                    civilId = civilId,
                    size = pageSize,
                    page = 0
                )

                result.fold(
                    onSuccess = { requestsResult ->
                        _requests.value = requestsResult.requests
                        _paginationState.value = requestsResult.pagination

                        println("✅ RequestsViewModel: Loaded ${requestsResult.requests.size} requests")
                    },
                    onFailure = { error ->
                        println("❌ RequestsViewModel: Failed to load requests: ${error.message}")
                        handleError(error)
                    }
                )
            } catch (e: ApiException) {
                // ✅ Handle API errors with proper error code
                println("❌ API Error in loadRequests: ${e.code} - ${e.message}")

                if (e.code == 401) {
                    println("🔐 401 Unauthorized - Token expired or invalid")
                    _appError.value = AppError.Unauthorized(e.message ?: "انتهت صلاحية الجلسة. الرجاء تحديث الرمز للمتابعة")
                } else {
                    _appError.value = AppError.ApiError(e.code, e.message ?: "حدث خطأ في الخادم")
                }
            } catch (e: Exception) {
                println("❌ RequestsViewModel: Exception: ${e.message}")
                _appError.value = AppError.Unknown(e.message ?: "حدث خطأ أثناء تحميل الطلبات")
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * Load more requests (next page)
     */
    fun loadMoreRequests() {
        if (_isLoadingMore.value || _paginationState.value.isLastPage) {
            return
        }

        viewModelScope.launch {
            try {
                _isLoadingMore.value = true
                _appError.value = null

                val civilId = currentCivilId ?: UserHelper.getOwnerCivilId(appContext)

                // ✅ Check if civil ID is null
                if (civilId == null) {
                    println("⚠️ RequestsViewModel: No token found when loading more")
                    _isLoadingMore.value = false
                    _appError.value = AppError.Unauthorized("لم يتم العثور على رمز الدخول. الرجاء تسجيل الدخول للمتابعة")
                    return@launch
                }

                val nextPage = _paginationState.value.currentPage + 1

                val result = requestsStrategy.loadUserRequests(
                    civilId = civilId,
                    size = pageSize,
                    page = nextPage
                )

                result.fold(
                    onSuccess = { requestsResult ->
                        val currentRequests = _requests.value.toMutableList()
                        currentRequests.addAll(requestsResult.requests)
                        _requests.value = currentRequests
                        _paginationState.value = requestsResult.pagination
                    },
                    onFailure = { error ->
                        println("❌ RequestsViewModel: Failed to load more: ${error.message}")
                        handleError(error)
                    }
                )
            } catch (e: ApiException) {
                // ✅ Handle API errors
                println("❌ API Error in loadMoreRequests: ${e.code} - ${e.message}")

                if (e.code == 401) {
                    _appError.value = AppError.Unauthorized(e.message ?: "انتهت صلاحية الجلسة")
                } else {
                    _appError.value = AppError.ApiError(e.code, e.message ?: "حدث خطأ في الخادم")
                }
            } catch (e: Exception) {
                println("❌ Exception in loadMoreRequests: ${e.message}")
                _appError.value = AppError.Unknown(e.message ?: "حدث خطأ أثناء تحميل المزيد")
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    /**
     * Refresh requests
     */
    fun refreshRequests() {
        viewModelScope.launch {
            try {
                _appError.value = null
                requestsStrategy.refreshRequests()
                loadRequests()
            } catch (e: Exception) {
                println("❌ Exception in refreshRequests: ${e.message}")
                _appError.value = AppError.Unknown(e.message ?: "فشل التحديث")
            }
        }
    }

    /**
     * ✅ UPDATED: Refresh expired access token (matching MainCategoriesViewModel pattern)
     * Called by UI when user clicks "Refresh Token" button in 401 error banner
     *
     * Flow:
     * 1. Try to refresh token
     * 2. If success → Clear error and automatically retry API call
     * 3. If fail → Show error with "Go to Login" option
     */
    fun refreshToken() {
        viewModelScope.launch {
            val result = authRepository.refreshAccessToken()

            result.fold(
                onSuccess = {
                    println("✅ Token refreshed successfully in RequestsViewModel")
                    _appError.value = null  // Clear error banner
                    // Retry the API call automatically
                    loadRequests()
                },
                onFailure = {
                    println("❌ Token refresh failed in RequestsViewModel")
                    // Show error with "Go to Login" button
                    _appError.value = AppError.Unknown("انتهت صلاحية رمز التحديث. يرجى تسجيل الدخول مرة أخرى")
                    // Don't auto-navigate, let user click the button
                }
            )
        }
    }

    /**
     * ✅ NEW: Trigger navigation to login (like MainCategoriesViewModel)
     */
    fun navigateToLogin() {
        _shouldNavigateToLogin.value = true
    }

    /**
     * ✅ NEW: Reset navigation trigger (like MainCategoriesViewModel)
     */
    fun resetNavigationTrigger() {
        _shouldNavigateToLogin.value = false
    }

    /**
     * ✅ NEW: Handle different error types
     */
    private fun handleError(error: Throwable) {
        _appError.value = when (error) {
            is ApiException -> {
                if (error.code == 401) {
                    AppError.Unauthorized(error.message ?: "انتهت صلاحية الجلسة")
                } else {
                    AppError.ApiError(error.code, error.message ?: "حدث خطأ في الخادم")
                }
            }
            else -> AppError.Unknown(error.message ?: "حدث خطأ غير متوقع")
        }
    }

    /**
     * Clear AppError state
     */
    fun clearAppError() {
        _appError.value = null
    }

    /**
     * Handle request item click
     * Determines navigation based on request status and type
     */
    fun onRequestClick(request: UserRequestUiModel) {
        println("🔘 RequestsViewModel: Request clicked - ID: ${request.id}, Status: ${request.statusName}, StatusID: ${request.statusId}")

        // Check if status is DRAFT (statusId = 1)
        val isDraft = request.statusId == 1

        if (isDraft) {
            // TODO: Navigate to transaction screen for editing draft
            println("📝 RequestsViewModel: Draft request - navigate to transaction screen (TODO)")
            // This will be implemented later when we integrate with transaction flows
            // For now, navigate to API detail screen
            _navigationToRequestDetail.value = Pair(request.id, request.requestTypeId)
        } else {
            // Navigate to API request detail screen
            println("📄 RequestsViewModel: Non-draft request - navigate to API detail screen")
            _navigationToRequestDetail.value = Pair(request.id, request.requestTypeId)
        }
    }

    /**
     * Clear navigation trigger
     */
    fun clearNavigationTrigger() {
        _navigationToRequestDetail.value = null
    }

    /**
     * ✅ NEW: Get status counts by customer ID
     * Fetches status counts from API for the current user
     */
    fun getStatusCounts() {
        viewModelScope.launch {
            try {
                _isLoadingStatusCounts.value = true

                val civilId = currentCivilId ?: UserHelper.getOwnerCivilId(appContext)

                if (civilId == null) {
                    println("⚠️ RequestsViewModel: No token found when fetching status counts")
                    _isLoadingStatusCounts.value = false
                    return@launch
                }

                println("📱 RequestsViewModel: Fetching status counts for civilId=$civilId")

                val result = requestsApiService.getStatusCounts(civilId)

                result.fold(
                    onSuccess = { response ->
                        println("✅ RequestsViewModel: Status counts loaded successfully")
                        _statusCountData.value = response.data
                        println("📊 Total Count: ${response.data?.totalCount}")
                        response.data?.statusCounts?.forEach { statusCount ->
                            println("📋 StatusId=${statusCount.statusId}: count=${statusCount.count}")
                        }
                    },
                    onFailure = { error ->
                        println("❌ RequestsViewModel: Failed to load status counts: ${error.message}")
                        // Don't show error to user, just use fallback values
                    }
                )
            } catch (e: ApiException) {
                println("❌ API Error in getStatusCounts: ${e.code} - ${e.message}")
                // Don't show error to user for status counts
            } catch (e: Exception) {
                println("❌ Exception in getStatusCounts: ${e.message}")
                // Don't show error to user for status counts
            } finally {
                _isLoadingStatusCounts.value = false
            }
        }
    }
}
