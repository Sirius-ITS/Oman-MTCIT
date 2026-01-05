package com.informatique.mtcit.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.informatique.mtcit.common.ApiException
import com.informatique.mtcit.common.AppError
import com.informatique.mtcit.data.model.requests.RequestDetailParser
import com.informatique.mtcit.data.model.requests.RequestDetailUiModel
import com.informatique.mtcit.data.model.requests.RequestTypeEndpoint
import com.informatique.mtcit.data.repository.UserRequestsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Request Detail Screen
 * Fetches and manages request detail data dynamically based on request type
 */
@HiltViewModel
class RequestDetailViewModel @Inject constructor(
    private val userRequestsRepository: UserRequestsRepository
) : ViewModel() {

    private val _requestDetail = MutableStateFlow<RequestDetailUiModel?>(null)
    val requestDetail: StateFlow<RequestDetailUiModel?> = _requestDetail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _appError = MutableStateFlow<AppError?>(null)
    val appError: StateFlow<AppError?> = _appError.asStateFlow()

    /**
     * Fetch request detail by ID and type
     */
    fun fetchRequestDetail(requestId: Int, requestTypeId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _appError.value = null

                println("🔍 RequestDetailViewModel: Fetching detail for requestId=$requestId, typeId=$requestTypeId")

                // Get endpoint path from request type mapping
                val endpointPath = RequestTypeEndpoint.getEndpointByTypeId(requestTypeId)

                if (endpointPath == null) {
                    println("❌ RequestDetailViewModel: Unsupported request type ID: $requestTypeId")
                    _appError.value = AppError.Unknown("نوع الطلب غير مدعوم")
                    _isLoading.value = false
                    return@launch
                }

                println("📡 RequestDetailViewModel: Using endpoint: $endpointPath")

                // Fetch from repository
                val result = userRequestsRepository.getRequestDetail(
                    requestId = requestId,
                    endpointPath = endpointPath
                )

                result.fold(
                    onSuccess = { response ->
                        println("✅ RequestDetailViewModel: Detail fetched successfully")

                        // Parse dynamic JSON to UI model
                        val uiModel = RequestDetailParser.parseToUiModel(response)
                        _requestDetail.value = uiModel

                        println("✅ RequestDetailViewModel: Parsed ${uiModel.sections.size} sections")
                    },
                    onFailure = { error ->
                        println("❌ RequestDetailViewModel: Error fetching detail: ${error.message}")

                        when (error) {
                            is ApiException -> {
                                when (error.code) {
                                    401 -> _appError.value = AppError.Unauthorized(
                                        error.message ?: "انتهت صلاحية الجلسة"
                                    )
                                    403 -> _appError.value = AppError.ApiError(
                                        error.code,
                                        "ليس لديك صلاحية للوصول"
                                    )
                                    404 -> _appError.value = AppError.ApiError(
                                        error.code,
                                        "الطلب غير موجود"
                                    )
                                    else -> _appError.value = AppError.ApiError(
                                        error.code,
                                        error.message ?: "حدث خطأ في الخادم"
                                    )
                                }
                            }
                            else -> {
                                _appError.value = AppError.Unknown(
                                    error.message ?: "حدث خطأ غير متوقع"
                                )
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                println("❌ RequestDetailViewModel: Exception: ${e.message}")
                _appError.value = AppError.Unknown(e.message ?: "حدث خطأ غير متوقع")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _appError.value = null
    }

    /**
     * Retry loading
     */
    fun retry(requestId: Int, requestTypeId: Int) {
        fetchRequestDetail(requestId, requestTypeId)
    }
}

