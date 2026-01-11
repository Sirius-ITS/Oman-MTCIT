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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

/**
 * Certificate data returned from issuance API
 */
data class CertificateData(
    val certificationNumber: String,
    val issuedDate: String,
    val expiryDate: String?,
    val certificationQrCode: String  // Base64 encoded PNG
)

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

    // ✅ NEW: Issuance-specific loading state
    private val _isIssuingCertificate = MutableStateFlow(false)
    val isIssuingCertificate: StateFlow<Boolean> = _isIssuingCertificate.asStateFlow()

    // ✅ NEW: Certificate data (replaces issuanceSuccess string)
    private val _certificateData = MutableStateFlow<CertificateData?>(null)
    val certificateData: StateFlow<CertificateData?> = _certificateData.asStateFlow()

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

                        // Parse dynamic JSON to UI model (pass known requestTypeId for inspection requests)
                        val uiModel = RequestDetailParser.parseToUiModel(response, requestTypeId)
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

    /**
     * ✅ NEW: Issue certificate for a request
     * Called when isPaid == 1 and status is APPROVED
     */
    fun issueCertificate(requestId: Int, requestTypeId: Int) {
        viewModelScope.launch {
            try {
                _isIssuingCertificate.value = true
                _appError.value = null
                _certificateData.value = null

                println("🔍 RequestDetailViewModel: Issuing certificate for requestId=$requestId, typeId=$requestTypeId")

                // Get issuance endpoint from mapping
                val issuanceEndpoint = RequestTypeEndpoint.getIssuanceEndpoint(requestTypeId, requestId)

                if (issuanceEndpoint == null) {
                    println("❌ RequestDetailViewModel: Issuance not supported for type ID: $requestTypeId")
                    _appError.value = AppError.Unknown("إصدار الشهادة غير مدعوم لهذا النوع")
                    _isIssuingCertificate.value = false
                    return@launch
                }

                println("📡 RequestDetailViewModel: Using issuance endpoint: $issuanceEndpoint")

                // Call issuance API
                val result = userRequestsRepository.issueCertificate(
                    issuanceEndpoint = issuanceEndpoint
                )

                result.fold(
                    onSuccess = { response ->
                        println("✅ RequestDetailViewModel: Certificate issued successfully")
                        println("📄 Response message: ${response.message}")

                        // ✅ Parse certificate data from response
                        try {
                            val dataObject = response.data.jsonObject
                            val certificationNumber = dataObject["certificationNumber"]?.jsonPrimitive?.content ?: ""
                            val issuedDate = dataObject["issuedDate"]?.jsonPrimitive?.content ?: ""
                            val expiryDate = dataObject["expiryDate"]?.jsonPrimitive?.content
                            val certificationQrCode = dataObject["certificationQrCode"]?.jsonPrimitive?.content ?: ""

                            println("✅ Parsed certificate data:")
                            println("   - Certificate Number: $certificationNumber")
                            println("   - Issued Date: $issuedDate")
                            println("   - Expiry Date: $expiryDate")
                            println("   - QR Code length: ${certificationQrCode.length}")

                            _certificateData.value = CertificateData(
                                certificationNumber = certificationNumber,
                                issuedDate = issuedDate,
                                expiryDate = expiryDate,
                                certificationQrCode = certificationQrCode
                            )

                            // Optionally refresh the request detail to update status
                            fetchRequestDetail(requestId, requestTypeId)
                        } catch (e: Exception) {
                            println("❌ Error parsing certificate data: ${e.message}")
                            _appError.value = AppError.Unknown("خطأ في معالجة بيانات الشهادة")
                        }
                    },
                    onFailure = { error ->
                        println("❌ RequestDetailViewModel: Error issuing certificate: ${error.message}")

                        when (error) {
                            is ApiException -> {
                                when (error.code) {
                                    401 -> _appError.value = AppError.Unauthorized(
                                        error.message ?: "انتهت صلاحية الجلسة"
                                    )
                                    403 -> _appError.value = AppError.ApiError(
                                        error.code,
                                        "ليس لديك صلاحية لإصدار هذه الشهادة"
                                    )
                                    404 -> _appError.value = AppError.ApiError(
                                        error.code,
                                        "الطلب غير موجود"
                                    )
                                    else -> _appError.value = AppError.ApiError(
                                        error.code,
                                        error.message ?: "حدث خطأ في إصدار الشهادة"
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
                println("❌ RequestDetailViewModel: Exception during issuance: ${e.message}")
                _appError.value = AppError.Unknown(e.message ?: "حدث خطأ غير متوقع")
            } finally {
                _isIssuingCertificate.value = false
            }
        }
    }

    /**
     * Clear certificate data (e.g., when dialog is dismissed)
     */
    fun clearCertificateData() {
        _certificateData.value = null
    }
}

