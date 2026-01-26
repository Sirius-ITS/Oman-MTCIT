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
    private val userRequestsRepository: UserRequestsRepository,
    private val checklistApiService: com.informatique.mtcit.data.api.ChecklistApiService
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

    /**
     * ✅ Clear toast message after showing
     */
    fun clearToastMessage() {
        _toastMessage.value = null
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
}
