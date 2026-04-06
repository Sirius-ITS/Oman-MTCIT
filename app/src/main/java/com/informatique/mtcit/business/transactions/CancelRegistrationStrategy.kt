package com.informatique.mtcit.business.transactions

//Imports
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.informatique.mtcit.R
import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.PaymentManager
import com.informatique.mtcit.business.transactions.shared.ReviewManager
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.business.transactions.shared.StepType
import com.informatique.mtcit.common.ApiException
import com.informatique.mtcit.common.ErrorMessageExtractor
import com.informatique.mtcit.data.model.cancelRegistration.DeletionFileUpload
import com.informatique.mtcit.data.model.cancelRegistration.DeletionSubmitResponse
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.data.repository.MarineUnitRepository
import com.informatique.mtcit.ui.components.PersonType
import com.informatique.mtcit.ui.components.SelectableItem
import com.informatique.mtcit.ui.repo.CompanyRepo
import com.informatique.mtcit.ui.viewmodels.StepData
import com.informatique.mtcit.util.UserHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import com.informatique.mtcit.common.util.AppLanguage

/**
 * Strategy for Cancel Permanent Registration (Deletion/Removal)
 * DEMONSTRATION: Highly simplified - minimal fields, just cancellation info
 * Shows extreme case of field removal for streamlined process
 */
class CancelRegistrationStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val companyRepository: CompanyRepo,
    private val validationUseCase: FormValidationUseCase,
    private val marineUnitRepository: MarineUnitRepository,
    private val lookupRepository: LookupRepository,
    private val marineUnitsApiService: com.informatique.mtcit.data.api.MarineUnitsApiService,
    private val shipSelectionManager: com.informatique.mtcit.business.transactions.shared.ShipSelectionManager,
    private val reviewManager: ReviewManager,
    private val paymentManager: PaymentManager,
    @ApplicationContext private val appContext: Context  // ✅ Injected context
) : BaseTransactionStrategy() {

    // ✅ Transaction context with all API endpoints
    private val transactionContext: TransactionContext = TransactionType.CANCEL_PERMANENT_REGISTRATION.context
    private val requestTypeId = TransactionType.CANCEL_PERMANENT_REGISTRATION.toRequestTypeId()
    private var portOptions: List<String> = emptyList()
    private var countryOptions: List<String> = emptyList()
    private var shipTypeOptions: List<String> = emptyList()
    private var commercialOptions: List<SelectableItem> = emptyList()
    private var typeOptions: List<PersonType> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()
    private var deletionReasonOptions: List<String> = emptyList() // ✅ NEW: Dynamic deletion reasons
    private var deletionReasonMap: Map<String, Int> = emptyMap() // ✅ NEW: Map name to ID
    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf() // ✅ Track form data// Store API error to prevent navigation and show error dialog
    private var lastApiError: String? = null
    private var requiredDocuments: List<com.informatique.mtcit.data.model.RequiredDocumentItem> = emptyList() // ✅ NEW: Store required documents from API
    private var deletionRequestId: Int? = null // ✅ NEW: Store created deletion request ID

    // ✅ INFINITE SCROLL: pagination state
    private var _currentShipsPage: Int = -1
    private var _isLastShipsPage: Boolean = true
    override val currentShipsPage: Int get() = _currentShipsPage
    override val isLastShipsPage: Boolean get() = _isLastShipsPage

    /**
     * ✅ Override setHasAcceptanceFromApi to also store in formData
     * This ensures the payment success dialog can access it
     */
    override fun setHasAcceptanceFromApi(hasAcceptanceValue: Int?) {
        super.setHasAcceptanceFromApi(hasAcceptanceValue)
        // ✅ Store in formData so PaymentSuccessDialog can access it
        accumulatedFormData["hasAcceptance"] = (hasAcceptanceValue == 1).toString()
        println("🔧 CancelRegistrationStrategy: Stored hasAcceptance in formData: ${accumulatedFormData["hasAcceptance"]}")
    }

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        // Load all dropdown options from API
        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val shipTypes = lookupRepository.getShipTypes().getOrNull() ?: emptyList()
        val ownerCivilId = UserHelper.getOwnerCivilId(appContext)
        val commercialRegistrations = if (ownerCivilId != null)
            lookupRepository.getCommercialRegistrations(ownerCivilId).getOrNull() ?: emptyList()
        else emptyList()
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()

        // ✅ NEW: Fetch deletion reasons from API
        try {
            val deletionReasonsResult = repository.getDeletionReasons()
            deletionReasonsResult.onSuccess { reasonsResponse ->
                // Create map of name -> id for later lookup
                val reasonsList = reasonsResponse.data?.content?.mapNotNull { item ->
                    val name = if (AppLanguage.isArabic) item?.nameAr else (item?.nameEn ?: item?.nameAr)
                    val id = item?.id
                    if (name != null && id != null) {
                        println("🗑️ Loaded reason: '$name' -> ID: $id")
                        Pair(name, id)
                    } else {
                        println("⚠️ Skipping reason with null name or id: $item")
                        null
                    }
                } ?: emptyList()

                deletionReasonOptions = reasonsList.map { it.first }
                deletionReasonMap = reasonsList.toMap()

                println("🗑️ ========== Deletion Reasons Loaded ==========")
                println("🗑️ Total reasons: ${deletionReasonOptions.size}")
                println("🗑️ Deletion Reason Options: $deletionReasonOptions")
                println("🗑️ Deletion Reason Map: $deletionReasonMap")
                println("🗑️ ============================================")
            }.onFailure { error ->
                println("❌ Failed to fetch deletion reasons: ${error.message}")
                val msg = when (error) {
                    is ApiException -> error.message ?: if (AppLanguage.isArabic) "فشل في جلب أسباب الحذف" else "Failed to fetch deletion reasons"
                    else -> ErrorMessageExtractor.extract(error.message)
                }

                // Store for UI/debugging and ensure it bubbles up
                accumulatedFormData["apiError"] = msg
                lastApiError = msg

                if (error is ApiException) throw error else throw ApiException(500, msg)
            }
        } catch (e: Exception) {
            println("❌ Exception fetching deletion reasons: ${e.message}")
            e.printStackTrace()
        }

        // ✅ Fetch required documents من الـ API
        println("📄 CancelRegistration - Fetching required documents from API...")
        val requiredDocumentsList = lookupRepository.getRequiredDocumentsByRequestType(requestTypeId)
            .getOrElse { error ->
                println("❌ ERROR fetching required documents: ${error.message}")
                error.printStackTrace()
                emptyList()
            }

        println("✅ Fetched ${requiredDocumentsList.size} required documents:")
        requiredDocumentsList.forEach { docItem ->
            val mandatoryText = if (docItem.document.isMandatory == 1) if (AppLanguage.isArabic) "إلزامي" else "Mandatory" else if (AppLanguage.isArabic) "اختياري" else "Optional"
            println("   - ${docItem.document.nameAr} ($mandatoryText)")
        }

        // Cache the options for use in getSteps()
        portOptions = ports
        countryOptions = countries
        shipTypeOptions = shipTypes
        commercialOptions = commercialRegistrations
        typeOptions = personTypes
        requiredDocuments = requiredDocumentsList // ✅ Store documents for later use

        println("🚢 Skipping initial ship load - will load after user selects type and presses Next")

        return mapOf(
            "marineUnits" to emptyList<MarineUnit>(),
            "registrationPort" to ports,
            "ownerNationality" to countries,
            "ownerCountry" to countries,
            "registrationCountry" to countries,
            "unitType" to shipTypes,
            "deletionReasons" to deletionReasonOptions, // ✅ NEW: Return deletion reasons
            "requiredDocuments" to requiredDocumentsList
        )
    }

    override suspend fun loadShipsForSelectedType(formData: Map<String, String>): List<MarineUnit> {
        val personType = formData["selectionPersonType"]
        // ✅ FIXED: The actual field ID is "selectionData" not "commercialRegistration"
        val commercialReg = formData["selectionData"]

        println("🚢 loadShipsForSelectedType called - personType=$personType, commercialReg=$commercialReg")

        // ✅ Get civilId from token instead of hardcoded value
        val ownerCivilIdFromToken = UserHelper.getOwnerCivilId(appContext)
        println("🔑 Owner CivilId from token: $ownerCivilIdFromToken")

        // ✅ UPDATED: For companies, use commercialReg (crNumber) from selectionData
        val (ownerCivilId, commercialRegNumber) = when (personType) {
            "فرد", "Individual" -> {
                println("✅ Individual: Using ownerCivilId from token")
                Pair(ownerCivilIdFromToken, null)
            }
            "شركة", "Company" -> {
                println("✅ Company: Using commercialRegNumber from selectionData = $commercialReg")
                Pair(ownerCivilIdFromToken, commercialReg) // ✅ Use civilId from token + commercialReg
            }
            else -> Pair(null, null)
        }

        println("🔍 Loading first page with loadShipsPage(page=0)")
        val firstPage = marineUnitRepository.loadShipsPage(
            ownerCivilId = ownerCivilId,
            commercialRegNumber = commercialRegNumber,
            requestTypeId = requestTypeId,
            page = 0
        )
        marineUnits = firstPage.ships
        _currentShipsPage = 0
        _isLastShipsPage = firstPage.isLastPage
        println("✅ Loaded ${marineUnits.size} ships (isLast=$_isLastShipsPage)")
        return marineUnits
    }

    override suspend fun clearLoadedShips() {
        println("🧹 Clearing loaded ships cache")
        marineUnits = emptyList()
        _currentShipsPage = -1
        _isLastShipsPage = true
    }

    /**
     * ✅ INFINITE SCROLL: Append next page of ships and rebuild steps.
     */
    override suspend fun loadNextShipsPage(formData: Map<String, String>) {
        if (_isLastShipsPage) return
        val nextPage = _currentShipsPage + 1
        val ownerCivilId = UserHelper.getOwnerCivilId(appContext)
        val commercialReg = formData["selectionData"]?.takeIf { it.isNotBlank() }
        println("📄 loadNextShipsPage (CancelRegistration) page=$nextPage")
        val result = marineUnitRepository.loadShipsPage(
            ownerCivilId = ownerCivilId,
            commercialRegNumber = commercialReg,
            requestTypeId = requestTypeId,
            page = nextPage
        )
        if (result.ships.isNotEmpty()) {
            marineUnits = marineUnits + result.ships
            _currentShipsPage = nextPage
            _isLastShipsPage = result.isLastPage
            onStepsNeedRebuild?.invoke()
        }
    }

    override fun updateAccumulatedData(data: Map<String, String>) {
        accumulatedFormData.putAll(data)
        println("📦 CancelRegistration - Updated accumulated data: $accumulatedFormData")
    }

    // ✅ NEW: Return current form data including payment WebView trigger flags
    override fun getFormData(): Map<String, String> {
        return accumulatedFormData.toMap()
    }

    override fun getContext(): TransactionContext {
        return transactionContext
    }

    override fun getSteps(): List<StepData> {
        val steps = mutableListOf<StepData>()

        // Step 1: Person Type
        steps.add(SharedSteps.personTypeStep(typeOptions))

        // Step 2: Commercial Registration (only for companies)
        val selectedPersonType = accumulatedFormData["selectionPersonType"]
        if (selectedPersonType == "شركة" || selectedPersonType == "Company") {
            steps.add(SharedSteps.commercialRegistrationStep(commercialOptions))
        }

        // Step 3: Marine Unit Selection
        steps.add(
            SharedSteps.marineUnitSelectionStep(
                units = marineUnits,
                allowMultipleSelection = false,
                showOwnedUnitsWarning = true
            )
        )

        // ✅ Step 4: Cancellation Reason + Documents (dynamic)
        steps.add(
            SharedSteps.createCancellationReasonStep(
                deletionReasons = deletionReasonOptions,
                requiredDocuments = requiredDocuments
            )
        )

        // Step 5: Review
        steps.add(SharedSteps.reviewStep())

        val hasRequestId = accumulatedFormData["requestId"] != null

        if (hasRequestId) {
            // Payment Details Step - Shows payment breakdown
            steps.add(SharedSteps.paymentDetailsStep(accumulatedFormData))

            // Payment Success Step - Only show if payment was successful
            val paymentSuccessful = accumulatedFormData["paymentSuccessful"]?.toBoolean() == true
            if (paymentSuccessful) {
                steps.add(SharedSteps.paymentSuccessStep())
            }
        }

        return steps
    }

    /**
     * ✅ Called when a step is opened - triggers payment API call when payment step is opened
     */
    override suspend fun onStepOpened(stepIndex: Int) {
        val step = getSteps().getOrNull(stepIndex) ?: return

        // ✅ If this is the payment step, trigger payment API call
        if (step.stepType == StepType.PAYMENT) {
            println("💰 Payment step opened - triggering payment receipt API call...")

            val paymentResult = paymentManager.processStepIfNeeded(
                stepType = StepType.PAYMENT,
                formData = accumulatedFormData,
                requestTypeId = requestTypeId.toInt(),
                context = transactionContext
            )

            when (paymentResult) {
                is com.informatique.mtcit.business.transactions.shared.StepProcessResult.Success -> {
                    println("✅ Payment receipt loaded - triggering step rebuild")
                    onStepsNeedRebuild?.invoke()
                }
                is com.informatique.mtcit.business.transactions.shared.StepProcessResult.Error -> {
                    println("❌ Payment error: ${paymentResult.message}")
                    accumulatedFormData["apiError"] = paymentResult.message
                }
                is com.informatique.mtcit.business.transactions.shared.StepProcessResult.NoAction -> {
                    println("ℹ️ No payment action needed at this time")
                }
            }
        }

        onStepsNeedRebuild?.invoke()
    }

    override fun validateStep(step: Int, data: Map<String, Any>): Pair<Boolean, Map<String, String>> {
        val stepData = getSteps().getOrNull(step) ?: return Pair(false, emptyMap())
        val formData = data.mapValues { it.value.toString() }
        return validationUseCase.validateStepWithAccumulatedData(
            stepData = stepData,
            currentStepData = formData,
            allAccumulatedData = accumulatedFormData,
            crossFieldRules = emptyList()
        )
    }

    override suspend fun processStepData(step: Int, data: Map<String, String>): Int {
        println("📄 processStepData called with: $data")

        // ✅ Accumulate form data
        accumulatedFormData.putAll(data)
        println("📦 CancelRegistration - Accumulated data: $accumulatedFormData")

        // Clear previous error
        lastApiError = null

        // Check current step
        val currentSteps = getSteps()
        val currentStepData = currentSteps.getOrNull(step)
        val stepType = currentStepData?.stepType

        println("🔍 Current step titleRes: ${currentStepData?.titleRes}")
        println("🔍 Current step type: $stepType")

        // ✅ NEW: Check if we just completed the Marine Unit Selection step
        if (currentStepData?.titleRes == R.string.owned_ships) {
            println("🚢 ✅ Marine Unit Selection step completed - using ShipSelectionManager...")
            try {
                // ✅ Use ShipSelectionManager
                val result = shipSelectionManager.handleShipSelection(
                    shipId = data["selectedMarineUnits"],
                    context = transactionContext
                )

                when (result) {
                    is com.informatique.mtcit.business.transactions.shared.ShipSelectionResult.Success -> {
                        println("✅ Ship selection successful!")
                        deletionRequestId = result.requestId
                        accumulatedFormData["requestId"] = result.requestId.toString()
                        accumulatedFormData["createdRequestId"] = result.requestId.toString()

                        // ✅ Extract and store shipInfoId for payment
                        val selectedUnitsJson = data["selectedMarineUnits"]
                        if (selectedUnitsJson != null) {
                            try {
                                val cleanJson = selectedUnitsJson.trim().removeSurrounding("[", "]")
                                val shipIds = cleanJson.split(",").map { it.trim().removeSurrounding("\"") }
                                val firstShipId = shipIds.firstOrNull()
                                if (firstShipId != null) {
                                    accumulatedFormData["shipInfoId"] = firstShipId
                                    accumulatedFormData["coreShipsInfoId"] = firstShipId
                                    println("✅ Stored shipInfoId: $firstShipId")
                                }
                            } catch (e: Exception) {
                                println("⚠️ Failed to extract shipInfoId: ${e.message}")
                            }
                        }
                    }
                    is com.informatique.mtcit.business.transactions.shared.ShipSelectionResult.Error -> {
                        println("❌ Ship selection failed: ${result.message}")
                        lastApiError = result.message
                        accumulatedFormData["apiError"] = result.message
                        // ✅ Throw exception to trigger error banner display
                        throw ApiException(500, result.message)
                    }
                }
            } catch (e: ApiException) {
                println("❌ ApiException in ship selection: ${e.message}")
                lastApiError = e.message ?: "Unknown error"
                accumulatedFormData["apiError"] = e.message ?: "Unknown error"
                throw e // Re-throw to show error banner
            } catch (e: Exception) {
                println("❌ Exception in ship selection: ${e.message}")
                val errorMsg = com.informatique.mtcit.common.ErrorMessageExtractor.extract(e.message)
                lastApiError = errorMsg
                accumulatedFormData["apiError"] = errorMsg
                throw ApiException(500, errorMsg)
            }
        }

        // ✅ The cancellation reason step has titleRes = R.string.cancellation_reason
        if (currentStepData?.titleRes == R.string.cancellation_reason) {
            println("🗑️ ✅ Cancellation Reason step completed - calling API...")

            var apiCallSucceeded = false
            try {
                // ✅ Collect all uploaded documents from dynamic fields
                val uploadedDocuments = collectUploadedDocuments(accumulatedFormData)

                println("📋 Total documents to upload: ${uploadedDocuments.size}")

                // ✅ Call the API with documents
                val result = submitDeletionWithFiles(accumulatedFormData, uploadedDocuments)

                result.fold(
                    onSuccess = { response ->
                        println("✅ Deletion request created successfully!")
                        println("   Deletion Request ID: ${response.data?.id}")
                        println("   Response: ${response.message}")

                        // ✅ CRITICAL: Store the deletion request ID in the member variable
                        deletionRequestId = response.data?.id
                        println("💾 STORED deletionRequestId = $deletionRequestId")

                        // ✅ CRITICAL FIX: Update the requestId that will be used in Review/Payment steps
                        if (deletionRequestId != null) {
                            accumulatedFormData["requestId"] = deletionRequestId.toString()
                            accumulatedFormData["createdRequestId"] = deletionRequestId.toString()
                            accumulatedFormData["deletionRequestId"] = deletionRequestId.toString()
                            println("💾 UPDATED accumulatedFormData['requestId'] = $deletionRequestId (will be used for Review/Payment)")
                        }

                        // Store success flag
                        accumulatedFormData["submissionSuccess"] = "true"
                        lastApiError = null
                        apiCallSucceeded = true
                    },

                    onFailure=  { error ->
                    println("❌ Failed to add crew: ${error.message}")
                    // Store API error for UI / debugging
                    val msg = when (error) {
                        is com.informatique.mtcit.common.ApiException -> error.message ?: if (AppLanguage.isArabic) "فشل في إضافة الطاقم" else "Failed to add crew"
                        else -> error.message ?: if (AppLanguage.isArabic) "فشل في إضافة الطاقم" else "Failed to add crew"
                    }
                    accumulatedFormData["apiError"] = msg
                    lastApiError = msg

                    // Re-throw as ApiException so upstream processStepData will catch and surface banner
                    if (error is com.informatique.mtcit.common.ApiException) {
                        throw error
                    } else {
                        throw com.informatique.mtcit.common.ApiException(400, msg)
                    }
                }
                )
            } catch (e: Exception) {
                println("❌ Exception while creating deletion request: ${e.message}")
                e.printStackTrace()

                lastApiError = e.message ?: if (AppLanguage.isArabic) "حدث خطأ غير متوقع" else "An unexpected error occurred"
                apiCallSucceeded = false
            }

            // ✅ Return -1 to prevent navigation if API call failed
            if (!apiCallSucceeded) {
                println("⚠️ API call failed - returning -1 to prevent navigation")
                return -1
            }
        }

        // ✅ NEW: Handle REVIEW step using ReviewManager
        if (stepType == StepType.REVIEW) {
            println("📋 Handling Review Step using ReviewManager")
            println("🔍 DEBUG: accumulatedFormData['requestId'] = ${accumulatedFormData["requestId"]}")
            println("🔍 DEBUG: deletionRequestId = $deletionRequestId")

            val requestIdInt = accumulatedFormData["requestId"]?.toIntOrNull()
            if (requestIdInt == null) {
                println("❌ No requestId available for review step")
                println("❌ accumulatedFormData keys: ${accumulatedFormData.keys}")
                lastApiError = if (AppLanguage.isArabic) "لم يتم العثور على رقم الطلب" else "Request number not found"
                return -1
            }

            println("✅ Using requestId: $requestIdInt for send-request API call")

            try {
                val endpoint = transactionContext.sendRequestEndpoint
                val contextName = transactionContext.displayName

                println("🚀 Calling ReviewManager.processReviewStep:")
                println("   Endpoint: $endpoint")
                println("   RequestId: $requestIdInt")
                println("   Context: $contextName")

                val result = reviewManager.processReviewStep(
                    endpoint = endpoint,
                    requestId = requestIdInt,
                    transactionName = contextName,
                    sendRequestPostOrPut = transactionContext.sendRequestPostOrPut
                )

                when (result) {
                    is com.informatique.mtcit.business.transactions.shared.ReviewResult.Success -> {
                        println("✅ Review step processed successfully!")
                        println("   Message: ${result.message}")
                        println("   Has Acceptance: ${result.hasAcceptance}")

                        // Store response in formData
                        accumulatedFormData["sendRequestMessage"] = result.message
                        accumulatedFormData["hasAcceptance"] = result.hasAcceptance.toString()

                        // ✅ Extract request number
                        val requestNumber = result.additionalData?.get("requestNumber")?.toString()
                            ?: result.additionalData?.get("requestSerial")?.toString()
                            ?: accumulatedFormData["requestSerial"]
                            ?: "N/A"

                        // ✅ NEW: Check if this is a NEW request
                        val isNewRequest = accumulatedFormData["requestId"] == null ||
                                          accumulatedFormData["isResumedTransaction"]?.toBoolean() != true

                        println("🔍 Post-submission flow decision:")
                        println("   - isNewRequest: $isNewRequest")
                        println("   - hasAcceptance (from API): ${result.hasAcceptance}")

                        // ✅ Only stop if BOTH isNewRequest AND hasAcceptance are true
                        if (isNewRequest && result.hasAcceptance) {
                            println("🎉 NEW cancel registration request submitted with hasAcceptance=true - showing success dialog and stopping")
                            println("   User must continue from profile screen")

                            // Set success flags for ViewModel to show dialog
                            accumulatedFormData["requestSubmitted"] = "true"
                            accumulatedFormData["requestNumber"] = requestNumber
                            accumulatedFormData["successMessage"] = result.message

                            // Return -2 to indicate: success but show dialog and stop
                            return -2
                        } else if (isNewRequest && !result.hasAcceptance) {
                            println("✅ NEW cancel registration request submitted with hasAcceptance=false - continuing to next steps")
                            println("   Transaction will continue to payment/next steps")
                            // Continue normally - don't return, let the flow proceed
                        } else {
                            println("✅ Resumed request - cancel registration request submitted successfully")
                        }
                    }
                    is com.informatique.mtcit.business.transactions.shared.ReviewResult.Error -> {
                        println("❌ Review step failed: ${result.message}")
                        lastApiError = result.message
                        return -1
                    }
                }
            } catch (e: com.informatique.mtcit.common.ApiException) {
                // ✅ Re-throw ApiException (e.g. 401) so ViewModel can show refresh button
                println("❌ ApiException in review step: ${e.code} - ${e.message}")
                throw e
            } catch (e: Exception) {
                println("❌ Exception in review step: ${e.message}")
                e.printStackTrace()
                lastApiError = e.message ?: if (AppLanguage.isArabic) "حدث خطأ أثناء مراجعة الطلب" else "An error occurred while reviewing the request"
                return -1
            }
        }

        // ✅ NEW: Handle PAYMENT step using PaymentManager
        if (stepType == StepType.PAYMENT) {
            println("💰 Handling Payment Step using PaymentManager")

            val paymentResult = paymentManager.processStepIfNeeded(
                stepType = stepType,
                formData = accumulatedFormData,
                requestTypeId = requestTypeId.toInt(),
                context = transactionContext
            )

            when (paymentResult) {
                is com.informatique.mtcit.business.transactions.shared.StepProcessResult.Success -> {
                    println("✅ Payment step processed: ${paymentResult.message}")

                    // Check if payment was submitted successfully
                    val showPaymentSuccessDialog = accumulatedFormData["showPaymentSuccessDialog"]?.toBoolean() ?: false
                    if (showPaymentSuccessDialog) {
                        println("✅ Payment submitted successfully - dialog will be shown")
                        return step
                    }
                }
                is com.informatique.mtcit.business.transactions.shared.StepProcessResult.Error -> {
                    println("❌ Payment step failed: ${paymentResult.message}")
                    lastApiError = paymentResult.message
                    return -1
                }
                is com.informatique.mtcit.business.transactions.shared.StepProcessResult.NoAction -> {
                    println("ℹ️ No payment action needed")
                }
            }
        }

        return step
    }

    /**
     * Get the last API error message if any
     * Used by UI to display error dialogs
     */
    fun getLastApiError(): String? = lastApiError

    /**
     * Clear the last API error
     */
    fun clearLastApiError() {
        lastApiError = null
    }

    // ✅ NEW: Collect uploaded documents from form data
    private suspend fun collectUploadedDocuments(
        formData: Map<String, String>
    ): List<DeletionFileUpload> {
        val uploadedDocuments = mutableListOf<DeletionFileUpload>()

        // ✅ Use the injected appContext (guaranteed non-null via Hilt)
        println("✅ Using context: ${appContext.javaClass.simpleName}")

        // Get all document fields (document_43, document_44, etc.)
        requiredDocuments
            .filter { it.document.isActive == 1 }
            .forEach { docItem ->
                val fieldId = "document_${docItem.document.id}"
                val documentUri = formData[fieldId]

                if (!documentUri.isNullOrBlank() && documentUri.startsWith("content://")) {
                    try {
                        val uri = android.net.Uri.parse(documentUri)

                        val bytes = appContext.contentResolver.openInputStream(uri)?.use {
                            it.readBytes()
                        } ?: throw Exception("Unable to read file")

                        val fileName = getFileNameFromUri(appContext, uri)
                            ?: "document_${docItem.document.id}_${System.currentTimeMillis()}"

                        val mimeType = appContext.contentResolver.getType(uri)
                            ?: "application/octet-stream"

                        val deletionFile = DeletionFileUpload(
                            fileName = fileName,
                            fileBytes = bytes,
                            mimeType = mimeType,
                            fileUri = uri.toString(),
                            docOwnerId = "document_${docItem.document.id}",
                            docId = docItem.document.id
                        )

                        uploadedDocuments.add(deletionFile)
                        println("📎 Added document: ${docItem.document.nameAr} (id=${docItem.document.id}, file=$fileName, size=${bytes.size} bytes)")

                    } catch (e: Exception) {
                        println("⚠️ Failed to process document ${docItem.document.nameAr}: ${e.message}")
                        e.printStackTrace()
                    }
                } else {
                    println("⚠️ Skipping document ${docItem.document.nameAr}: invalid URI '$documentUri'")
                }
            }

        return uploadedDocuments
    }

    // Helper function to get filename from URI
    private fun getFileNameFromUri(context: Context, uri: android.net.Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun submitDeletionWithFiles(
        data: Map<String, String>,
        files: List<DeletionFileUpload>
    ): Result<DeletionSubmitResponse> {
        return try {
            println("📤 ========== submitDeletionWithFiles ==========")
            println("📤 Form data keys: ${data.keys}")

            // Extract deletion reason ID
            val deletionReasonName = data["cancellationReason"]
                ?: return Result.failure(Exception(if (AppLanguage.isArabic) "سبب الشطب مطلوب" else "Cancellation reason is required"))

            val deletionReasonId = deletionReasonMap[deletionReasonName]
                ?: return Result.failure(Exception(if (AppLanguage.isArabic) "سبب الشطب غير صحيح" else "Invalid cancellation reason"))

            // ✅ FIX: Extract ship info ID from correct field
            // The marine unit selection field stores data in "selectedMarineUnits" (JSON array)
            val selectedUnitsJson = data["selectedMarineUnits"]
                ?: return Result.failure(Exception(if (AppLanguage.isArabic) "السفينة مطلوبة" else "Ship is required"))

            println("🔍 Selected units JSON: $selectedUnitsJson")

            // Parse the JSON array to get the first marine unit ID
            val shipInfoId = try {
                // Remove brackets and quotes, split by comma, take first
                val cleanJson = selectedUnitsJson.trim().removeSurrounding("[", "]")
                val shipIds = cleanJson.split(",").map { it.trim().removeSurrounding("\"") }
                val firstShipId = shipIds.firstOrNull()

                if (firstShipId.isNullOrBlank()) {
                    println("❌ Failed to parse maritime ID from: $selectedUnitsJson")
                    return Result.failure(Exception("Invalid marine unit selection format"))
                }

                println("📍 Extracted ship ID: $firstShipId")

                // ✅ FIXED: The JSON contains shipInfoId directly, not maritimeId
                // Try to convert to Int directly
                val actualShipId = firstShipId.toIntOrNull()
                if (actualShipId == null) {
                    println("❌ Ship ID is not a valid integer: $firstShipId")
                    return Result.failure(Exception("Invalid ship ID format"))
                }

                // ✅ Optional: Find the MarineUnit for logging (not required for API call)
                val selectedUnit = marineUnits.firstOrNull { it.id == firstShipId }
                if (selectedUnit != null) {
                    println("✅ Found matching MarineUnit:")
                    println("   Ship ID: $actualShipId")
                    println("   Ship Name: ${selectedUnit.shipName}")
                    println("   Maritime ID (MMSI): ${selectedUnit.maritimeId}")
                    println("   IMO Number: ${selectedUnit.imoNumber}")
                } else {
                    println("⚠️ MarineUnit not found in cache, but using shipId: $actualShipId")
                }

                actualShipId
            } catch (e: Exception) {
                println("❌ Exception parsing selected units: ${e.message}")
                e.printStackTrace()
                return Result.failure(Exception("Failed to parse selected marine unit: ${e.message}"))
            }

            println("📤 Files received: ${files.size}")

            files.forEachIndexed { index, file ->
                println("📎 File $index: ${file.fileName} (${file.fileBytes.size} bytes)")
            }

            if (files.isEmpty()) {
                println("❌ ERROR: No files provided!")
                return Result.failure(Exception(if (AppLanguage.isArabic) "يجب إرفاق مستند واحد على الأقل" else "At least one document must be attached"))
            }

            println("📤 Submitting: reasonId=$deletionReasonId, shipId=$shipInfoId, files=${files.size}")

            // ✅ Call repository
            val result = repository.submitDeletionRequest(deletionReasonId, shipInfoId, files)

            result
        } catch (e: Exception) {
            println("❌ Exception: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        return repository.submitRegistration(data)
    }

    override fun handleFieldChange(fieldId: String, value: String, formData: Map<String, String>): Map<String, String> {
        // Handle owner type change
        if (fieldId == "owner_type") {
            val mutableFormData = formData.toMutableMap()
            when (value) {
                "فرد", "Individual" -> {
                    mutableFormData.remove("companyName")
                    mutableFormData.remove("companyRegistrationNumber")
                }
                "شركة", "Company" -> {
                    // Company fields will be shown and are required
                }
                "شراكة", "Partnership" -> {
                    mutableFormData.remove("companyName")
                    mutableFormData.remove("companyRegistrationNumber")
                }
            }
            return mutableFormData
        }
        return formData
    }

    override suspend fun onFieldFocusLost(fieldId: String, value: String): FieldFocusResult {
        if (fieldId == "companyRegistrationNumber") {
            return handleCompanyRegistrationLookup(value)
        }
        return FieldFocusResult.NoAction
    }

    private suspend fun handleCompanyRegistrationLookup(registrationNumber: String): FieldFocusResult {
        if (registrationNumber.isBlank()) {
            return FieldFocusResult.Error("companyRegistrationNumber", if (AppLanguage.isArabic) "رقم السجل التجاري مطلوب" else "Commercial registration number is required")
        }

        if (registrationNumber.length < 3) {
            return FieldFocusResult.Error(
                "companyRegistrationNumber",
                if (AppLanguage.isArabic) "رقم السجل التجاري يجب أن يكون أكثر من 3 أرقام" else "Commercial registration number must be more than 3 digits"
            )
        }

        return try {
            val result = companyRepository.fetchCompanyLookup(registrationNumber)
                .flowOn(Dispatchers.IO)
                .catch { throwable ->
                    throw Exception(if (AppLanguage.isArabic) "حدث خطأ أثناء البحث عن الشركة: ${throwable.message}" else "An error occurred while searching for the company: ${throwable.message}")
                }
                .first()

            when (result) {
                is BusinessState.Success -> {
                    val companyData = result.data.result
                    if (companyData != null) {
                        FieldFocusResult.UpdateFields(
                            mapOf(
                                "companyName" to companyData.arabicCommercialName,
                                "companyType" to companyData.commercialRegistrationEntityType
                            )
                        )
                    } else {
                        FieldFocusResult.Error("companyRegistrationNumber", if (AppLanguage.isArabic) "لم يتم العثور على الشركة" else "Company not found")
                    }
                }
                is BusinessState.Error -> {
                    FieldFocusResult.Error("companyRegistrationNumber", result.message)
                }
                is BusinessState.Loading -> {
                    FieldFocusResult.NoAction
                }
            }
        } catch (e: Exception) {
            FieldFocusResult.Error("companyRegistrationNumber", e.message ?: if (AppLanguage.isArabic) "حدث خطأ غير متوقع" else "An unexpected error occurred")
        }
    }

    fun getDeletionRequestId(): Int? {
        println("🔍 getDeletionRequestId called")
        println("   deletionRequestId = $deletionRequestId")
        println("   accumulatedFormData['deletionRequestId'] = ${accumulatedFormData["deletionRequestId"]}")

        // ✅ Fallback: Try to get from accumulated form data if member variable is null
        if (deletionRequestId == null) {
            val idFromFormData = accumulatedFormData["deletionRequestId"]?.toIntOrNull()
            if (idFromFormData != null) {
                println("⚠️ deletionRequestId was null, using value from formData: $idFromFormData")
                deletionRequestId = idFromFormData
            }
        }

        return deletionRequestId
    }

    // ================================================================================
    // 🎯 DRAFT TRACKING: Extract completed steps from API response
    // ================================================================================
    override fun extractCompletedStepsFromApiResponse(response: Any): Set<StepType> {
        // TODO: Parse the cancel registration API response
        val completedSteps = mutableSetOf<StepType>()

        println("⚠️ CancelRegistrationStrategy: extractCompletedStepsFromApiResponse not yet implemented")
        println("   Response type: ${response::class.simpleName}")

        return completedSteps
    }
}
