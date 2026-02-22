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
import kotlinx.serialization.json.jsonArray
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
 * Mapping of request type IDs to certificate type IDs
 * ✅ Based on TransactionType.typeId and actual API certificate types
 */
private val REQUEST_TYPE_TO_CERTIFICATE_TYPE = mapOf(
    1 to 1,  // Temp Registration (typeId=1) -> Provisional registration certificate (certificationType.id=1)
    2 to 2,  // Perm Registration (typeId=2) -> Permanent registration certificate (certificationType.id=2)
    3 to 3,  // Issue Navigation Permit (typeId=3) -> Navigation Permit Certificate (certificationType.id=3)
    4 to 6,  // Mortgage Certificate (typeId=4) -> شهادة الرهن (certificationType.id=6) ✅
    5 to null, // Release Mortgage (typeId=5) -> NO certificate issued (just releases existing mortgage)
    6 to 5,  // Renew Navigation Permit (typeId=6) -> Navigation Renew Permit Certificate (certificationType.id=5) ✅
    7 to 7,  // Cancel Permanent Registration (typeId=7) -> Cancellation Certificate (certificationType.id=7)
    8 to null,  // Request Inspection (typeId=8) -> NO certificate issued
    12 to null  // Change Port of Ship (typeId=12) -> Certificate type TBD (may issue certificate showing port change)
)

/**
 * ViewModel for Request Detail Screen
 * Fetches and manages request detail data dynamically based on request type
 */
@HiltViewModel
class RequestDetailViewModel @Inject constructor(
    private val userRequestsRepository: UserRequestsRepository,
    private val checklistApiService: com.informatique.mtcit.data.api.ChecklistApiService,
    private val authRepository: com.informatique.mtcit.data.repository.AuthRepository // ✅ NEW: For token refresh
) : ViewModel() {

    private val _requestDetail = MutableStateFlow<RequestDetailUiModel?>(null)
    val requestDetail: StateFlow<RequestDetailUiModel?> = _requestDetail.asStateFlow()

    // ✅ Store raw response to access shipCertifications and other nested data
    private val _rawResponse = MutableStateFlow<com.informatique.mtcit.data.model.requests.RequestDetailResponse?>(null)
    val rawResponse: StateFlow<com.informatique.mtcit.data.model.requests.RequestDetailResponse?> = _rawResponse.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _appError = MutableStateFlow<AppError?>(null)
    val appError: StateFlow<AppError?> = _appError.asStateFlow()

    // ✅ NEW: Issuance-specific loading state
    private val _isIssuingCertificate = MutableStateFlow(false)
    val isIssuingCertificate: StateFlow<Boolean> = _isIssuingCertificate.asStateFlow()

    // ✅ NEW: Track if we're in "view mode" to prevent showing issuance dialog
    private val _isViewingCertificate = MutableStateFlow(false)
    val isViewingCertificate: StateFlow<Boolean> = _isViewingCertificate.asStateFlow()

    // ✅ NEW: Certificate data (replaces issuanceSuccess string)
    private val _certificateData = MutableStateFlow<CertificateData?>(null)
    val certificateData: StateFlow<CertificateData?> = _certificateData.asStateFlow()

    // ✅ NEW: Certificate URL for opening in external browser
    private val _certificateUrl = MutableStateFlow<String?>(null)
    val certificateUrl: StateFlow<String?> = _certificateUrl.asStateFlow()

    // ✅ NEW: Navigation to login trigger (when refresh token expires)
    private val _shouldNavigateToLogin = MutableStateFlow(false)
    val shouldNavigateToLogin: StateFlow<Boolean> = _shouldNavigateToLogin.asStateFlow()

    // ✅ NEW: Checklist items state
    private val _checklistItems = MutableStateFlow<List<com.informatique.mtcit.data.model.ChecklistItem>>(emptyList())
    val checklistItems: StateFlow<List<com.informatique.mtcit.data.model.ChecklistItem>> = _checklistItems.asStateFlow()

    private val _isLoadingChecklist = MutableStateFlow(false)
    val isLoadingChecklist: StateFlow<Boolean> = _isLoadingChecklist.asStateFlow()

    // ✅ NEW: Inspection decisions state
    private val _inspectionDecisions = MutableStateFlow<List<com.informatique.mtcit.data.model.InspectionDecision>>(emptyList())
    val inspectionDecisions: StateFlow<List<com.informatique.mtcit.data.model.InspectionDecision>> = _inspectionDecisions.asStateFlow()

    private val _isLoadingDecisions = MutableStateFlow(false)
    val isLoadingDecisions: StateFlow<Boolean> = _isLoadingDecisions.asStateFlow()

    // ✅ NEW: Checklist answers state (to track if all mandatory fields are filled)
    private val _checklistAnswers = MutableStateFlow<Map<Int, String>>(emptyMap())
    val checklistAnswers: StateFlow<Map<Int, String>> = _checklistAnswers.asStateFlow()

    // ✅ NEW: Work order result ID state (stored after first draft save)
    private val _workOrderResultId = MutableStateFlow<Int?>(null)
    val workOrderResultId: StateFlow<Int?> = _workOrderResultId.asStateFlow()

    // ✅ NEW: Answer IDs state (stored after first draft save for subsequent updates)
    private val _answerIds = MutableStateFlow<Map<Int, Int>>(emptyMap()) // checklistItemId -> answerId
    val answerIds: StateFlow<Map<Int, Int>> = _answerIds.asStateFlow()

    // ✅ NEW: Toast message state (for non-blocking success messages)
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // ✅ NEW: File viewer dialog state
    data class FileViewerState(
        val isOpen: Boolean = false,
        val fileUri: String = "",
        val fileName: String = "",
        val mimeType: String = ""
    )

    private val _fileViewerState = MutableStateFlow(FileViewerState())
    val fileViewerState: StateFlow<FileViewerState> = _fileViewerState.asStateFlow()

    /**
     * ✅ Clear toast message after showing
     */
    fun clearToastMessage() {
        _toastMessage.value = null
    }

    /**
     * ✅ Open file viewer dialog with URL
     */
    fun openFileViewerDialog(url: String, fileName: String, mimeType: String) {
        println("📂 RequestDetailViewModel: Opening file viewer")
        println("   URL: $url")
        println("   File: $fileName")
        println("   Type: $mimeType")
        _fileViewerState.value = FileViewerState(
            isOpen = true,
            fileUri = url,
            fileName = fileName,
            mimeType = mimeType
        )
    }

    /**
     * ✅ Close file viewer dialog
     */
    fun closeFileViewerDialog() {
        println("📂 RequestDetailViewModel: Closing file viewer")
        _fileViewerState.value = FileViewerState()
        _isViewingCertificate.value = false  // ✅ Reset viewing flag when file viewer closes
    }

    /**
     * ✅ Set checklist items directly (used when items come from workOrderResult)
     */
    fun setChecklistItems(items: List<com.informatique.mtcit.data.model.ChecklistItem>) {
        _checklistItems.value = items
        println("✅ RequestDetailViewModel: Set ${items.size} checklist items directly")
    }

    /**
     * ✅ Set work order result ID from API response (when loading existing draft)
     */
    fun setWorkOrderResultId(id: Int) {
        _workOrderResultId.value = id
        println("✅ RequestDetailViewModel: Set work order result ID: $id")
    }

    /**
     * ✅ Set existing checklist answers from workOrderResult
     */
    fun setChecklistAnswers(answers: Map<Int, String>) {
        _checklistAnswers.value = answers
        println("✅ RequestDetailViewModel: Set ${answers.size} checklist answers")
    }

    /**
     * ✅ Set answer IDs from existing workOrderResult
     */
    fun setAnswerIds(answerIds: Map<Int, Int>) {
        _answerIds.value = answerIds
        println("✅ RequestDetailViewModel: Set ${answerIds.size} answer IDs")
    }

    /**
     * Fetch request detail by ID and type
     * @param requestId Request ID
     * @param requestTypeId Request type ID
     * @param isEngineer Whether the user is an engineer (uses different API)
     */
    fun fetchRequestDetail(requestId: Int, requestTypeId: Int, isEngineer: Boolean = false) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _appError.value = null

                println("🔍 RequestDetailViewModel: Fetching detail for requestId=$requestId, typeId=$requestTypeId, isEngineer=$isEngineer")

                // ✅ Use engineer API if user is engineer
                val result = if (isEngineer) {
                    println("📡 RequestDetailViewModel: Using ENGINEER API")
                    userRequestsRepository.getEngineerRequestDetail(requestId)
                } else {
                    // Get endpoint path from request type mapping
                    val endpointPath = RequestTypeEndpoint.getEndpointByTypeId(requestTypeId)

                    if (endpointPath == null) {
                        println("❌ RequestDetailViewModel: Unsupported request type ID: $requestTypeId")
                        _appError.value = AppError.Unknown("نوع الطلب غير مدعوم")
                        _isLoading.value = false
                        return@launch
                    }

                    println("📡 RequestDetailViewModel: Using CLIENT API with endpoint: $endpointPath")

                    // Fetch from repository
                    userRequestsRepository.getRequestDetail(
                        requestId = requestId,
                        endpointPath = endpointPath
                    )
                }

                result.fold(
                    onSuccess = { response ->
                        println("✅ RequestDetailViewModel: Detail fetched successfully")

                        // ✅ Store raw response for later use (to access shipCertifications)
                        _rawResponse.value = response

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
    fun retry(requestId: Int, requestTypeId: Int, isEngineer: Boolean = false) {
        fetchRequestDetail(requestId, requestTypeId, isEngineer)
    }

    /**
     * ✅ NEW: Issue certificate for a request
     * Called when isPaid == 1 and status is APPROVED or ISSUED
     */
    fun issueCertificate(requestId: Int, requestTypeId: Int, statusId: Int = 0) {
        println("════════════════════════════════════════════════════════════")
        println("🚀 issueCertificate() CALLED")
        println("   requestId: $requestId")
        println("   requestTypeId: $requestTypeId")
        println("   statusId: $statusId")
        println("════════════════════════════════════════════════════════════")

        viewModelScope.launch {
            try {
                _isIssuingCertificate.value = true
                _appError.value = null
                _certificateData.value = null
                _isViewingCertificate.value = false  // ✅ Reset viewing flag (we're issuing, not just viewing)
                println("✅ Set _isIssuingCertificate = true")

                println("🔍 RequestDetailViewModel: Issuing/Showing certificate for requestId=$requestId, typeId=$requestTypeId, statusId=$statusId")

                // ✅ Extract ship certifications from stored raw response
                println("📦 Calling extractShipCertifications()...")
                val shipCertifications = extractShipCertifications()
                println("📦 extractShipCertifications() returned: ${shipCertifications?.size ?: 0} certificates")

                // ✅ Check if certificate is already issued (statusId == 14)
                if (statusId == 14 && shipCertifications != null) {
                    println("✅ Certificate already ISSUED (statusId==14) - fetching existing certificate")

                    // Map requestTypeId to certificationType.id
                    val certificateTypeId = mapRequestTypeToCertificateType(requestTypeId)
                    println("🔍 Mapped requestTypeId=$requestTypeId to certificateTypeId=$certificateTypeId")

                    // Find the certificate number from shipCertifications
                    println("🔍 Calling findCertificateNumber() with requestId=$requestId...")
                    val certificationNumber = findCertificateNumber(shipCertifications, certificateTypeId, requestId)
                    println("🔍 findCertificateNumber() returned: $certificationNumber")

                    if (certificationNumber == null) {
                        println("❌ Could not find certificate number for type: $certificateTypeId, requestId: $requestId")
                        _appError.value = AppError.Unknown("لم يتم العثور على الشهادة")
                        _isIssuingCertificate.value = false
                        return@launch
                    }

                    println("✅ Found certificate number: $certificationNumber")

                    // Fetch the certificate from API
                    println("📡 Calling getCertificate API with certificationNumber: $certificationNumber")
                    val result = userRequestsRepository.getCertificate(certificationNumber)
                    println("📡 getCertificate API returned")

                    result.fold(
                        onSuccess = { response ->
                            println("✅ Certificate fetched successfully")
                            println("📄 Response: ${response.message}")
                            parseCertificateResponse(response)
                        },
                        onFailure = { error ->
                            println("❌ Failed to fetch certificate: ${error.message}")
                            error.printStackTrace()
                            _appError.value = AppError.Unknown(error.message ?: "فشل في جلب الشهادة")
                        }
                    )

                    _isIssuingCertificate.value = false
                    println("✅ Set _isIssuingCertificate = false")
                    println("════════════════════════════════════════════════════════════")
                    return@launch
                }

                // ✅ Original flow: Issue new certificate
                println("🔍 Issuing new certificate (statusId != 14 or no shipCertifications)")

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
                        parseCertificateResponse(response)

                        // Optionally refresh the request detail to update status
                        fetchRequestDetail(requestId, requestTypeId)
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
     * ✅ Extract ship certifications from stored raw response
     */
    private fun extractShipCertifications(): List<kotlinx.serialization.json.JsonObject>? {
        return try {
            val response = _rawResponse.value ?: return null
            val dataObject = response.data.jsonObject
            val shipInfo = dataObject["shipInfo"]?.jsonObject ?: return null
            val shipCerts = shipInfo["shipCertifications"]?.jsonArray ?: return null

            shipCerts.mapNotNull { it as? kotlinx.serialization.json.JsonObject }
        } catch (e: Exception) {
            println("⚠️ Error extracting shipCertifications: ${e.message}")
            null
        }
    }

    /**
     * ✅ Map requestTypeId to certificationType.id
     * Based on the certificate type mapping:
     * - Temp Registration (typeId=1) → Certificate Type 1
     * - Perm Registration (typeId=2) → Certificate Type 2
     * - Issue Navigation (typeId=3) → Certificate Type 3
     * - Renew Navigation (typeId=6) → Certificate Type 5
     * - Mortgage (typeId=4) → Certificate Type 6 (شهادة الرهن)
     * - Release Mortgage (typeId=5) → Certificate Type (TBD)
     * - Cancel Registration (typeId=7) → Certificate Type (TBD)
     */
    private fun mapRequestTypeToCertificateType(requestTypeId: Int): Int {
        return when (requestTypeId) {
            1 -> 1  // Temp Registration → شهادة التسجيل المؤقتة
            2 -> 2  // Perm Registration → شهادة التسجيل الدائمة
            3 -> 3  // Issue Navigation → شهادة تصريح ملاحي
            6 -> 5  // Renew Navigation → شهادة تجديد تصريح ملاحي
            4 -> 6  // Mortgage → شهادة رهن (Certificate Type 6)
            5 -> 7  // Release Mortgage → شهادة فك رهن (assuming type 7)
            7 -> 8  // Cancel Registration → شهادة إلغاء تسجيل (assuming type 8)
            8 -> 9  // Inspection → شهادة معاينة (assuming type 9)
            else -> requestTypeId  // Fallback to same ID
        }
    }

    /**
     * ✅ Find certificate number from shipCertifications array
     * First tries to match by certificateTypeId, then falls back to matching by requestId
     */
    private fun findCertificateNumber(shipCertifications: List<Any>, certificateTypeId: Int, requestId: Int? = null): String? {
        try {
            // First pass: Try to find by certificationType.id
            for (cert in shipCertifications) {
                // Parse as JsonObject
                val certJson = when (cert) {
                    is kotlinx.serialization.json.JsonObject -> cert
                    else -> continue
                }

                // Get certificationType
                val certificationType = certJson["certificationType"]?.jsonObject
                val typeId = certificationType?.get("id")?.jsonPrimitive?.content?.toIntOrNull()

                println("🔍 Checking certificate: typeId=$typeId, looking for $certificateTypeId")

                if (typeId == certificateTypeId) {
                    val certNumber = certJson["certificationNumber"]?.jsonPrimitive?.content
                    println("✅ Found matching certificate by type: $certNumber")
                    return certNumber
                }
            }

            // Second pass: If requestId is provided, try to find by requestId
            if (requestId != null) {
                println("⚠️ Certificate not found by type, trying to match by requestId=$requestId")
                for (cert in shipCertifications) {
                    val certJson = when (cert) {
                        is kotlinx.serialization.json.JsonObject -> cert
                        else -> continue
                    }

                    val certRequestId = certJson["requestId"]?.jsonPrimitive?.content?.toIntOrNull()
                    println("🔍 Checking certificate: requestId=$certRequestId")

                    if (certRequestId == requestId) {
                        val certNumber = certJson["certificationNumber"]?.jsonPrimitive?.content
                        println("✅ Found matching certificate by requestId: $certNumber")
                        return certNumber
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ Error finding certificate number: ${e.message}")
        }
        return null
    }

    /**
     * ✅ Parse certificate response (common for both issuance and fetching)
     */
    private fun parseCertificateResponse(response: com.informatique.mtcit.data.model.requests.RequestDetailResponse) {
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
        } catch (e: Exception) {
            println("❌ Error parsing certificate data: ${e.message}")
            _appError.value = AppError.Unknown("خطأ في معالجة بيانات الشهادة")
        }
    }

    /**
     * Clear certificate data (e.g., when dialog is dismissed)
     */
    fun clearCertificateData() {
        _certificateData.value = null
    }

    /**
     * ✅ Clear certificate URL after opening in external browser
     */
    fun clearCertificateUrl() {
        _certificateUrl.value = null
    }

    /**
     * ✅ DEPRECATED: QR decoding is no longer needed
     * Certificate URLs are now constructed directly based on transaction type
     */
    @Deprecated("No longer needed - certificate URLs are constructed directly")
    fun decodeQrCode(qrCodeBase64: String): String? {
        println("⚠️ decodeQrCode is deprecated - URLs are now constructed directly")
        return null
    }

    /**
     * ✅ NEW: Construct certificate viewing URL from certification number
     * This allows viewing the certificate in a webview without decoding QR code
     */
    fun getCertificateViewUrl(certificationNumber: String): String {
        return "https://omanapi.isfpegypt.com/api/v1/certificate/$certificationNumber"
    }

    /**
     * ✅ NEW: Load checklist items by purpose ID (engineer only)
     * Called when engineer opens a scheduled inspection request
     */
    fun loadChecklistByPurpose(purposeId: Int) {
        viewModelScope.launch {
            try {
                _isLoadingChecklist.value = true

                println("🔍 RequestDetailViewModel: Loading checklist for purposeId=$purposeId")

                val result = checklistApiService.getChecklistByPurpose(purposeId)

                result.fold(
                    onSuccess = { response ->
                        println("✅ RequestDetailViewModel: Loaded ${response.data.items.size} checklist items")
                        _checklistItems.value = response.data.items.sortedBy { it.itemOrder }
                    },
                    onFailure = { error ->
                        println("❌ RequestDetailViewModel: Error loading checklist: ${error.message}")
                        // Don't set error state here - just log and continue
                        // The UI will show empty checklist if loading fails
                        _checklistItems.value = emptyList()
                    }
                )
            } catch (e: Exception) {
                println("❌ RequestDetailViewModel: Exception loading checklist: ${e.message}")
                _checklistItems.value = emptyList()
            } finally {
                _isLoadingChecklist.value = false
            }
        }
    }

    /**
     * ✅ NEW: Update checklist answer
     */
    fun updateChecklistAnswer(itemId: Int, answer: String) {
        val currentAnswers = _checklistAnswers.value.toMutableMap()
        currentAnswers[itemId] = answer
        _checklistAnswers.value = currentAnswers
        println("📝 Updated answer for item $itemId: $answer")
    }

    /**
     * ✅ NEW: Check if all mandatory fields are filled
     */
    fun areAllMandatoryFieldsFilled(): Boolean {
        val mandatoryItems = _checklistItems.value.filter { it.isMandatory }
        val answers = _checklistAnswers.value

        return mandatoryItems.all { item ->
            val answer = answers[item.id]
            !answer.isNullOrBlank()
        }
    }

    /**
     * ✅ NEW: Load inspection decisions
     */
    fun loadInspectionDecisions() {
        viewModelScope.launch {
            try {
                _isLoadingDecisions.value = true
                println("🔍 RequestDetailViewModel: Loading inspection decisions")

                val result = checklistApiService.getInspectionDecisions()

                result.fold(
                    onSuccess = { response ->
                        println("✅ RequestDetailViewModel: Loaded ${response.data.size} decisions")
                        _inspectionDecisions.value = response.data
                    },
                    onFailure = { error ->
                        println("❌ RequestDetailViewModel: Error loading decisions: ${error.message}")
                        _inspectionDecisions.value = emptyList()
                    }
                )
            } catch (e: Exception) {
                println("❌ RequestDetailViewModel: Exception loading decisions: ${e.message}")
                _inspectionDecisions.value = emptyList()
            } finally {
                _isLoadingDecisions.value = false
            }
        }
    }

    /**
     * ✅ NEW: Submit work order result
     * @param decisionId Decision ID (1=Accepted, 2=Refused, 3=Reinspection)
     * @param scheduledRequestId Scheduled request ID from requestDetail
     * @param expiredDate Expiry date (required only if decisionId = 1)
     */
    fun submitWorkOrderResult(
        decisionId: Int,
        scheduledRequestId: Int,
        expiredDate: String? = null
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _appError.value = null

                println("🔍 RequestDetailViewModel: Submitting work order result")
                println("   Decision ID: $decisionId")
                println("   Scheduled Request ID: $scheduledRequestId")
                println("   Expired Date: ${expiredDate ?: "N/A"}")

                // Build answers list from checklistAnswers and checklistItems
                val answers = _checklistAnswers.value.map { (itemId, answer) ->
                    // ✅ For choice-based items (List type, id=4), answer is already the choice ID as string
                    // For text-based items, answer is the text value
                    // API expects answer as string in both cases
                    val checklistItem = _checklistItems.value.find { it.id == itemId }

                    println("   📝 Processing answer for item $itemId:")
                    println("      Type: ${checklistItem?.checklistType?.nameEn}")
                    println("      Raw answer: $answer")

                    com.informatique.mtcit.data.model.WorkOrderAnswerSubmission(
                        answer = answer, // Already in correct format (ID for choice, text for text fields)
                        checklistSettingsItemId = itemId
                    )
                }

                println("   Answers: ${answers.size} items")
                answers.forEach { ans ->
                    println("      - Item ${ans.checklistSettingsItemId}: ${ans.answer}")
                }

                val request = com.informatique.mtcit.data.model.WorkOrderResultRequest(
                    decisionId = decisionId,
                    answers = answers,
                    scheduledRequestId = scheduledRequestId,
                    expiredDate = expiredDate
                )

                val result = checklistApiService.submitWorkOrderResult(request)

                result.fold(
                    onSuccess = { response ->
                        println("✅ RequestDetailViewModel: Work order result submitted successfully")
                        println("   Message: ${response.message}")

                        // ✅ Refresh request detail to show updated status
                        val currentDetail = _requestDetail.value
                        if (currentDetail != null) {
                            // Use the original requestId from the detail, not scheduledRequestId
                            fetchRequestDetail(currentDetail.requestId, currentDetail.requestType.id, true)
                        }
                    },
                    onFailure = { error ->
                        println("❌ RequestDetailViewModel: Error submitting work order result: ${error.message}")
                        _appError.value = AppError.Unknown(error.message ?: "فشل في حفظ نتيجة الفحص")
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
     * ✅ NEW: Save inspection as draft (POST if first time, PUT if updating)
     * @param scheduledRequestId Scheduled request ID from requestDetail
     */
    fun saveDraftInspection(scheduledRequestId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _appError.value = null

                val currentWorkOrderResultId = _workOrderResultId.value
                val currentAnswerIds = _answerIds.value

                println("💾 RequestDetailViewModel: Saving inspection as draft")
                println("   Scheduled Request ID: $scheduledRequestId")
                println("   Existing Work Order Result ID: $currentWorkOrderResultId")
                println("   Answers: ${_checklistAnswers.value.size} items")

                if (currentWorkOrderResultId == null) {
                    // ✅ First time save - use POST
                    println("   📤 First time save - using POST")

                    val answers = _checklistAnswers.value.map { (itemId, answer) ->
                        com.informatique.mtcit.data.model.DraftAnswerSubmission(
                            answer = answer,
                            checklistSettingsItemId = itemId
                        )
                    }

                    val request = com.informatique.mtcit.data.model.DraftSaveRequest(
                        scheduledRequestId = scheduledRequestId,
                        answers = answers
                    )

                    val result = checklistApiService.saveDraft(request)

                result.fold(
                    onSuccess = { response ->
                        println("✅ Draft created successfully")
                        println("    Work Order Result ID: ${response.data}")

                        // ✅ Store the newly created work order result ID
                        response.data?.let { newId ->
                            _workOrderResultId.value = newId
                            println("📝 Stored work order result ID: $newId")
                        }

                        // ✅ Show success toast
                        _toastMessage.value = "✅ تم حفظ المسودة بنجاح"

                        // ✅ Reload request details to show updated draft status
                        _requestDetail.value?.let { currentDetail ->
                            fetchRequestDetail(
                                requestId = currentDetail.requestId,
                                requestTypeId = currentDetail.requestType.id,
                                isEngineer = true
                            )
                        }
                    },
                    onFailure = { error ->
                        println("❌ Error creating draft: ${error.message}")
                        _toastMessage.value = "❌ ${error.message ?: "فشل في إنشاء المسودة"}"
                    }
                )
                } else {
                    // ✅ Subsequent save - use PUT
                    println("   📤 Updating existing draft - using PUT")

                    val answers = _checklistAnswers.value.map { (itemId, answer) ->
                        com.informatique.mtcit.data.model.DraftAnswerUpdateSubmission(
                            id = currentAnswerIds[itemId], // May be null for new answers
                            answer = answer,
                            checklistSettingsItemId = itemId
                        )
                    }

                    val request = com.informatique.mtcit.data.model.DraftUpdateRequest(
                        id = currentWorkOrderResultId,
                        scheduledRequestId = scheduledRequestId,
                        answers = answers
                    )

                    val result = checklistApiService.updateDraft(request)

                    result.fold(
                        onSuccess = { response ->
                            println("✅ Draft updated successfully")

                            // ✅ Show success toast (will be handled in UI)
                            _toastMessage.value = "✅ تم حفظ المسودة بنجاح"

                            // ✅ Reload request details to show updated draft status
                            _requestDetail.value?.let { currentDetail ->
                                fetchRequestDetail(
                                    requestId = currentDetail.requestId,
                                    requestTypeId = currentDetail.requestType.id,
                                    isEngineer = true
                                )
                            }
                        },
                        onFailure = { error ->
                            println("❌ Error updating draft: ${error.message}")
                            _toastMessage.value = "❌ ${error.message ?: "فشل في تحديث المسودة"}"
                        }
                    )
                }
            } catch (e: Exception) {
                println("❌ RequestDetailViewModel: Exception saving draft: ${e.message}")
                _toastMessage.value = "❌ ${e.message ?: "حدث خطأ غير متوقع"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ✅ NEW: Execute inspection submission (final submission)
     * This method:
     * 1. First saves/updates the draft (to ensure all answers are saved)
     * 2. Then calls execute API with the decision
     *
     * @param scheduledRequestId Scheduled request ID
     * @param decisionId Decision ID (1=Approved, 2=Refused, etc.)
     * @param refuseNotes Notes if refused
     * @param expiredDate Expiry date if approved
     */
    fun executeInspectionSubmission(
        scheduledRequestId: Int,
        decisionId: Int,
        refuseNotes: String = "",
        expiredDate: String? = null
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _appError.value = null

                println("✅ RequestDetailViewModel: Executing inspection submission")
                println("   Scheduled Request ID: $scheduledRequestId")
                println("   Decision ID: $decisionId")
                println("   Refuse Notes: $refuseNotes")
                println("   Expired Date: ${expiredDate ?: "N/A"}")

                // ✅ Step 1: Save/update draft first
                val currentWorkOrderResultId = _workOrderResultId.value
                val currentAnswerIds = _answerIds.value
                var finalWorkOrderResultId: Int? = currentWorkOrderResultId

                if (currentWorkOrderResultId == null) {
                    // ✅ First time - POST draft
                    println("   📤 Step 1: Saving draft (POST)")

                    val answers = _checklistAnswers.value.map { (itemId, answer) ->
                        com.informatique.mtcit.data.model.DraftAnswerSubmission(
                            answer = answer,
                            checklistSettingsItemId = itemId
                        )
                    }

                    val saveRequest = com.informatique.mtcit.data.model.DraftSaveRequest(
                        scheduledRequestId = scheduledRequestId,
                        answers = answers
                    )

                    val saveResult = checklistApiService.saveDraft(saveRequest)

                    saveResult.fold(
                        onSuccess = { response ->
                            println("   ✅ Draft saved, ID: ${response.data}")
                            finalWorkOrderResultId = response.data
                            _workOrderResultId.value = response.data
                        },
                        onFailure = { error ->
                            println("   ❌ Failed to save draft: ${error.message}")
                            _toastMessage.value = "❌ ${error.message ?: "فشل في حفظ المسودة"}"
                            _isLoading.value = false
                            return@launch
                        }
                    )
                } else {
                    // ✅ Update existing draft - PUT
                    println("   📤 Step 1: Updating draft (PUT)")

                    val answers = _checklistAnswers.value.map { (itemId, answer) ->
                        com.informatique.mtcit.data.model.DraftAnswerUpdateSubmission(
                            id = currentAnswerIds[itemId],
                            answer = answer,
                            checklistSettingsItemId = itemId
                        )
                    }

                    val updateRequest = com.informatique.mtcit.data.model.DraftUpdateRequest(
                        id = currentWorkOrderResultId,
                        scheduledRequestId = scheduledRequestId,
                        answers = answers
                    )

                    val updateResult = checklistApiService.updateDraft(updateRequest)

                    updateResult.fold(
                        onSuccess = { response ->
                            println("   ✅ Draft updated successfully")
                            // ✅ For PUT, keep using the existing work order result ID
                            finalWorkOrderResultId = currentWorkOrderResultId
                        },
                        onFailure = { error ->
                            println("   ❌ Failed to update draft: ${error.message}")
                            _toastMessage.value = "❌ ${error.message ?: "فشل في تحديث المسودة"}"
                            _isLoading.value = false
                            return@launch
                        }
                    )
                }

                // ✅ Step 2: Execute inspection with the work order result ID
                val workOrderIdToExecute = finalWorkOrderResultId
                if (workOrderIdToExecute != null) {
                    println("   📤 Step 2: Executing inspection (POST execute)")

                    val executeRequest = com.informatique.mtcit.data.model.ExecuteInspectionRequest(
                        decisionId = decisionId,
                        refuseNotes = refuseNotes,
                        expiredDate = expiredDate
                    )

                    val executeResult = checklistApiService.executeInspection(
                        workOrderResultId = workOrderIdToExecute,
                        request = executeRequest
                    )

                    executeResult.fold(
                        onSuccess = { response ->
                            println("   ✅ Inspection executed successfully")
                            println("      Message: ${response.message}")

                            // ✅ Show success toast
                            _toastMessage.value = "✅ تم تنفيذ الفحص بنجاح"

                            // ✅ Refresh request detail to show updated status
                            val currentDetail = _requestDetail.value
                            if (currentDetail != null) {
                                fetchRequestDetail(currentDetail.requestId, currentDetail.requestType.id, true)
                            }

                            // Clear stored IDs after successful submission
                            _workOrderResultId.value = null
                            _answerIds.value = emptyMap()
                        },
                        onFailure = { error ->
                            println("   ❌ Failed to execute inspection: ${error.message}")
                            _toastMessage.value = "❌ ${error.message ?: "فشل في تنفيذ الفحص"}"
                        }
                    )
                } else {
                    println("   ❌ No work order result ID available for execution")
                    _toastMessage.value = "❌ فشل في الحصول على معرف نتيجة الفحص"
                }
            } catch (e: Exception) {
                println("❌ RequestDetailViewModel: Exception executing inspection: ${e.message}")
                _toastMessage.value = "❌ ${e.message ?: "حدث خطأ غير متوقع"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ✅ NEW: Get certificate number for the current request based on status and request type
     * First tries to match by certificationType.id, then falls back to requestId matching
     */
    private fun getCertificationNumber(requestTypeId: Int, certificates: List<kotlinx.serialization.json.JsonObject>?, requestId: Int? = null): String? {
        if (certificates == null) return null

        // Map request type ID to certification type ID
        val certificationTypeId = REQUEST_TYPE_TO_CERTIFICATE_TYPE[requestTypeId]

        if (certificationTypeId == null) {
            println("⚠️ No certificate type mapping for request type ID: $requestTypeId")

            // Fallback: Try to find by requestId
            if (requestId != null) {
                println("🔍 Trying to find certificate by requestId: $requestId")
                val matchingCertificate = certificates.find { cert ->
                    val certRequestId = cert["requestId"]?.jsonPrimitive?.content?.toIntOrNull()
                    certRequestId == requestId
                }

                val certNumber = matchingCertificate?.get("certificationNumber")?.jsonPrimitive?.content
                if (certNumber != null) {
                    println("✅ Found certificate by requestId: $certNumber")
                } else {
                    println("❌ No certificate found for requestId: $requestId")
                }
                return certNumber
            }

            return null
        }

        println("🔍 Looking for certificate with type ID: $certificationTypeId")

        // Find certificate with matching certification type ID
        val matchingCertificate = certificates.find { cert ->
            val certType = cert["certificationType"]?.jsonObject
            val typeId = certType?.get("id")?.jsonPrimitive?.content?.toIntOrNull()
            typeId == certificationTypeId
        }

        val certNumber = matchingCertificate?.get("certificationNumber")?.jsonPrimitive?.content

        if (certNumber != null) {
            println("✅ Found certificate by type: $certNumber")
        } else {
            println("⚠️ No certificate found for type ID: $certificationTypeId")

            // Fallback: Try to find by requestId
            if (requestId != null) {
                println("🔍 Trying fallback: find by requestId: $requestId")
                val fallbackCertificate = certificates.find { cert ->
                    val certRequestId = cert["requestId"]?.jsonPrimitive?.content?.toIntOrNull()
                    certRequestId == requestId
                }

                val fallbackCertNumber = fallbackCertificate?.get("certificationNumber")?.jsonPrimitive?.content
                if (fallbackCertNumber != null) {
                    println("✅ Found certificate by requestId (fallback): $fallbackCertNumber")
                    return fallbackCertNumber
                } else {
                    println("❌ No certificate found for requestId: $requestId")
                }
            }
        }

        return certNumber
    }

    /**
     * ✅ View certificate for already issued requests
     * Constructs the certificate URL based on transaction type
     *
     * @param requestTypeId The type of request
     * @param useExternalBrowser If true, sets certificateUrl for external browser; if false, opens WebView
     */
    fun viewCertificate(requestTypeId: Int, useExternalBrowser: Boolean = false) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _isViewingCertificate.value = true  // ✅ Set viewing flag
                println("🔘 viewCertificate called for requestTypeId: $requestTypeId")
                println("   useExternalBrowser: $useExternalBrowser")

                val rawResponse = _rawResponse.value
                if (rawResponse == null) {
                    println("❌ No raw response available")
                    _toastMessage.value = "❌ لا توجد تفاصيل للطلب"
                    _isLoading.value = false
                    _isViewingCertificate.value = false
                    return@launch
                }

                // ✅ Get requestId from request detail
                val requestId = _requestDetail.value?.requestId
                if (requestId == null) {
                    println("❌ No request ID available")
                    _toastMessage.value = "❌ معرف الطلب غير متاح"
                    _isLoading.value = false
                    _isViewingCertificate.value = false
                    return@launch
                }

                // Extract shipCertifications from JsonElement
                val shipCertifications = try {
                    val dataObject = rawResponse.data.jsonObject
                    val shipInfoElement = dataObject["shipInfo"]

                    if (shipInfoElement != null) {
                        val shipInfo = shipInfoElement.jsonObject
                        val shipCertsArray = shipInfo["shipCertifications"]?.jsonArray

                        shipCertsArray?.map { it.jsonObject }
                    } else {
                        println("⚠️ No shipInfo in response")
                        null
                    }
                } catch (e: Exception) {
                    println("⚠️ Error extracting shipCertifications: ${e.message}")
                    e.printStackTrace()
                    null
                }

                // Get certificate number based on request type
                val certificationNumber = getCertificationNumber(requestTypeId, shipCertifications, requestId)

                if (certificationNumber != null) {
                    println("✅ Found certificate number: $certificationNumber")

                    // ✅ Construct certificate URL based on transaction type
                    val certificateUrl = getCertificateUrl(requestTypeId, certificationNumber, requestId)

                    if (certificateUrl != null) {
                        println("✅ Certificate URL constructed: $certificateUrl")

                        // ✅ SMART TOGGLE: Choose viewing method based on configuration
                        if (useExternalBrowser) {
                            // =============================================
                            // 🌐 EXTERNAL BROWSER APPROACH
                            // =============================================
                            println("🌐 Opening certificate in external browser")
                            _certificateUrl.value = certificateUrl  // UI will detect this and open external browser
                            _isLoading.value = false
                            _isViewingCertificate.value = false  // No need to keep viewing flag
                        } else {
                            // =============================================
                            // 📱 IN-APP WEBVIEW APPROACH (Default)
                            // =============================================
                            println("📱 Opening certificate in WebView dialog")
                            _fileViewerState.value = FileViewerState(
                                fileUri = certificateUrl,
                                fileName = "Certificate_$certificationNumber.html",
                                mimeType = "text/html",
                                isOpen = true
                            )

                            println("📂 RequestDetailViewModel: Opening file viewer")
                            println("   URL: $certificateUrl")
                            println("   File: Certificate_$certificationNumber.html")
                            println("   Type: text/html")

                            _isLoading.value = false
                            // ✅ Keep viewing flag true until file viewer is closed
                        }
                    } else {
                        println("❌ No certificate URL mapping for request type: $requestTypeId")
                        _toastMessage.value = "❌ لا يمكن عرض الشهادة لهذا النوع من الطلبات"
                        _isLoading.value = false
                        _isViewingCertificate.value = false
                    }
                } else {
                    println("❌ No certificate found for request type: $requestTypeId")
                    _toastMessage.value = "❌ لا توجد شهادة لهذا الطلب"
                    _isLoading.value = false
                    _isViewingCertificate.value = false
                }
            } catch (e: Exception) {
                println("❌ Error viewing certificate: ${e.message}")
                e.printStackTrace()
                _toastMessage.value = "❌ حدث خطأ أثناء عرض الشهادة"
                _isLoading.value = false
                _isViewingCertificate.value = false
            }
        }
    }

    /**
     * ✅ Get certificate URL based on transaction type
     */
    private fun getCertificateUrl(requestTypeId: Int, certificationNumber: String, requestId: Int): String? {
        val baseUrl = "https://oman.isfpegypt.com/services"

        return when (requestTypeId) {
            1 -> "$baseUrl/temporary-registration/cert?certificateNumber=$certificationNumber&requestId=$requestId" // Temp Registration
            2 -> "$baseUrl/permanent-registration/cert?certificateNumber=$certificationNumber&requestId=$requestId" // Perm Registration
            3 -> "$baseUrl/navigation-license/license-certificate?certificateNumber=$certificationNumber&requestId=$requestId" // Issue Navigation License
            4 -> "$baseUrl/mortgage-certificate/cert?certificateNumber=$certificationNumber&requestId=$requestId" // Mortgage Certificate
            5 -> "$baseUrl/mortgage-redemption/cert?certificateNumber=$certificationNumber&requestId=$requestId" // Release Mortgage
            6 -> "$baseUrl/navigation-license-renewal/renewal-license-certificate?certificateNumber=$certificationNumber&requestId=$requestId" // Renew Navigation License
            7 -> "$baseUrl/permanent-registration-cancellation/cert?certificateNumber=$certificationNumber&requestId=$requestId" // Cancel Registration
            8 -> null // Request Inspection - No certificate issuance
            12 -> "$baseUrl/change-port/cert?certificateNumber=$certificationNumber&requestId=$requestId" // Change Port of Ship
            else -> {
                println("⚠️ Unknown request type ID: $requestTypeId")
                null
            }
        }
    }

    /**
     * ✅ NEW: Refresh expired access token
     * Called by UI when user clicks "Refresh Token" button in 401 error banner
     *
     * Flow:
     * 1. Try to refresh token
     * 2. If success → Clear error and automatically retry API call
     * 3. If fail → Auto-navigate to login screen
     */
    fun refreshToken(requestId: Int, requestTypeId: Int, isEngineer: Boolean) {
        viewModelScope.launch {
            val result = authRepository.refreshAccessToken()

            result.fold(
                onSuccess = {
                    println("✅ Token refreshed successfully in RequestDetailViewModel")
                    _appError.value = null  // Clear error banner
                    // Retry the API call automatically
                    fetchRequestDetail(requestId, requestTypeId, isEngineer)
                },
                onFailure = {
                    println("❌ Token refresh failed in RequestDetailViewModel - navigating to login")
                    // ✅ Clear error banner BEFORE navigating to login
                    _appError.value = null
                    // ✅ AUTO-NAVIGATE to login
                    _shouldNavigateToLogin.value = true
                }
            )
        }
    }

    /**
     * ✅ NEW: Trigger navigation to login
     */
    fun navigateToLogin() {
        _shouldNavigateToLogin.value = true
    }

    /**
     * ✅ NEW: Reset navigation trigger
     */
    fun resetNavigationTrigger() {
        _shouldNavigateToLogin.value = false
    }

    /**
     * ✅ NEW: Clear error banner
     */
    fun clearAppError() {
        _appError.value = null
    }
}
