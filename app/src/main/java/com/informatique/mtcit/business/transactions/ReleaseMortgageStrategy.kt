package com.informatique.mtcit.business.transactions

import android.content.Context
import com.informatique.mtcit.R
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.business.transactions.shared.StepType
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.ui.components.PersonType
import com.informatique.mtcit.ui.components.SelectableItem
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.ui.viewmodels.StepData
import javax.inject.Inject
import com.informatique.mtcit.business.transactions.marineunit.rules.ReleaseMortgageRules
import com.informatique.mtcit.business.transactions.marineunit.usecases.ValidateMarineUnitUseCase
import com.informatique.mtcit.business.transactions.marineunit.usecases.GetEligibleMarineUnitsUseCase
import com.informatique.mtcit.common.ApiException
import com.informatique.mtcit.data.repository.MarineUnitRepository
import com.informatique.mtcit.data.api.MortgageApiService
import com.informatique.mtcit.data.helpers.FileUploadHelper
import com.informatique.mtcit.data.model.OwnerFileUpload
import com.informatique.mtcit.business.transactions.shared.StepProcessResult
import com.informatique.mtcit.util.UserHelper
import com.informatique.mtcit.common.ErrorMessageExtractor
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Strategy for Release Mortgage
 * Steps:
 * 1. Person Type Selection (Individual/Company)
 * 2. Commercial Registration (conditional - only for Company)
 * 3. Unit Selection (choose from mortgaged ships) - WITH BUSINESS VALIDATION
 * 4. Upload Documents (mortgage certificate)
 * 5. Review
 */
class ReleaseMortgageStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository,
    private val releaseMortgageRules: ReleaseMortgageRules,
    private val validateMarineUnitUseCase: ValidateMarineUnitUseCase,
    private val getEligibleUnitsUseCase: GetEligibleMarineUnitsUseCase,
    private val marineUnitRepository: MarineUnitRepository,
    private val marineUnitsApiService: com.informatique.mtcit.data.api.MarineUnitsApiService,
    private val mortgageApiService: MortgageApiService,
    private val reviewManager: com.informatique.mtcit.business.transactions.shared.ReviewManager,
    private val paymentManager: com.informatique.mtcit.business.transactions.shared.PaymentManager,
    private val shipSelectionManager: com.informatique.mtcit.business.transactions.shared.ShipSelectionManager,
    @ApplicationContext private val appContext: Context
) : BaseTransactionStrategy() {

    // ✅ Transaction context with all API endpoints
    private val transactionContext: TransactionContext = TransactionType.RELEASE_MORTGAGE.context

    private var portOptions: List<String> = emptyList()
    private var countryOptions: List<String> = emptyList()
    private var personTypeOptions: List<PersonType> = emptyList()
    private var commercialOptions: List<SelectableItem> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()
    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf()

    // ✅ INFINITE SCROLL: pagination state
    private var _currentShipsPage: Int = -1
    private var _isLastShipsPage: Boolean = true
    override val currentShipsPage: Int get() = _currentShipsPage
    override val isLastShipsPage: Boolean get() = _isLastShipsPage

    // ✅ NEW: Store required documents from API
    private var requiredDocuments: List<com.informatique.mtcit.data.model.RequiredDocumentItem> = emptyList()

    // ✅ Store the created redemption request ID for later status update
    private var createdRedemptionRequestId: Int? = null

    // ✅ NEW: Store API responses for future actions
    private val apiResponses: MutableMap<String, Any> = mutableMapOf()

    // ✅ Transaction context with all API endpoints
    private val context: TransactionContext = TransactionType.RELEASE_MORTGAGE.context
    private var lastApiError: String? = null
    private val requestTypeId = TransactionType.RELEASE_MORTGAGE.toRequestTypeId()

    /**
     * ✅ Override setHasAcceptanceFromApi to also store in formData
     * This ensures the payment success dialog can access it
     */
    override fun setHasAcceptanceFromApi(hasAcceptanceValue: Int?) {
        super.setHasAcceptanceFromApi(hasAcceptanceValue)
        // ✅ Store in formData so PaymentSuccessDialog can access it
        accumulatedFormData["hasAcceptance"] = (hasAcceptanceValue == 1).toString()
        println("🔧 ReleaseMortgageStrategy: Stored hasAcceptance in formData: ${accumulatedFormData["hasAcceptance"]}")
    }

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()
        val commercialRegistrations = lookupRepository.getCommercialRegistrations("12345678901234").getOrNull() ?: emptyList()

        println("📄 ReleaseMortgage - Fetching required documents from API...")
        val requestTypeId = TransactionType.RELEASE_MORTGAGE.toRequestTypeId()
        val requiredDocumentsList = lookupRepository.getRequiredDocumentsByRequestType(requestTypeId).getOrElse { error ->
            println("❌ ERROR fetching required documents: ${error.message}")
            error.printStackTrace()
            emptyList()
        }

        println("✅ Fetched ${requiredDocumentsList.size} required documents:")
        requiredDocumentsList.forEach { docItem ->
            val mandatoryText = if (docItem.document.isMandatory == 1) "إلزامي" else "اختياري"
            println("   - ${docItem.document.nameAr} ($mandatoryText)")
        }

        portOptions = ports
        countryOptions = countries
        personTypeOptions = personTypes
        commercialOptions = commercialRegistrations
        requiredDocuments = requiredDocumentsList // ✅ Store documents

        println("🚢 Skipping initial ship load - will load after user selects type and presses Next")

        return mapOf(
            "registrationPort" to ports,
            "ownerNationality" to countries,
            "ownerCountry" to countries,
            "bankCountry" to countries,
            "marineUnits" to emptyList<String>(),
            "commercialRegistration" to commercialRegistrations,
            "personType" to personTypes
        )
    }

    override suspend fun loadShipsForSelectedType(formData: Map<String, String>): List<MarineUnit> {
        val personType = formData["selectionPersonType"]
        // ✅ Extract CR Number from selectionData (not company name)
        val commercialReg = formData["selectionData"]

        println("🔒 loadShipsForSelectedType (RELEASE MORTGAGE) - personType=$personType, commercialReg=$commercialReg")

        // ✅ Get civilId from token instead of hardcoded value
        val ownerCivilIdFromToken = UserHelper.getOwnerCivilId(appContext)
        println("🔑 Owner CivilId from token: $ownerCivilIdFromToken")

        // ✅ For individuals: send ownerCivilId + requestTypeId ONLY (no commercialNumber)
        // ✅ For companies: send ownerCivilId + requestTypeId + commercialNumber (CR Number)
        val (ownerCivilId, commercialRegNumber) = when (personType) {
            "فرد" -> {
                println("✅ Individual: Using ownerCivilId from token")
                Pair(ownerCivilIdFromToken, null)
            }
            "شركة" -> {
                println("✅ Company: Using commercialRegNumber from selectionData = $commercialReg")
                Pair(ownerCivilIdFromToken, commercialReg) // ✅ Use civilId from token + commercialReg
            }
            else -> Pair(null, null)
        }

        println("🔍 Loading first page with loadShipsPage(page=0)")
        val firstPage = marineUnitRepository.loadShipsPage(
            ownerCivilId = ownerCivilId,
            commercialRegNumber = commercialRegNumber,
            requestTypeId = TransactionType.RELEASE_MORTGAGE.toRequestTypeId(),
            page = 0
        )
        marineUnits = firstPage.ships
        _currentShipsPage = 0
        _isLastShipsPage = firstPage.isLastPage
        println("✅ Loaded ${marineUnits.size} ships for Release Mortgage (isLast=$_isLastShipsPage)")
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
        println("📄 loadNextShipsPage (ReleaseMortgage) page=$nextPage")
        val result = marineUnitRepository.loadShipsPage(
            ownerCivilId = ownerCivilId,
            commercialRegNumber = commercialReg,
            requestTypeId = TransactionType.RELEASE_MORTGAGE.toRequestTypeId(),
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
        println("📦 ReleaseMortgage - Updated accumulated data: $accumulatedFormData")
    }

    // ✅ NEW: Return current form data including payment WebView trigger flags
    override fun getFormData(): Map<String, String> {
        return accumulatedFormData.toMap()
    }

    override fun getSteps(): List<StepData> {
        val steps = mutableListOf<StepData>()

        // Step 1: Person Type Selection
        steps.add(
            SharedSteps.personTypeStep(
                options = personTypeOptions
            )
        )

        // Step 2: Commercial Registration (only for companies)
        val selectedPersonType = accumulatedFormData["selectionPersonType"]
        if (selectedPersonType == "شركة") {
            steps.add(
                SharedSteps.commercialRegistrationStep(
                    options = commercialOptions
                )
            )
        }


        // Step 3: Marine Unit Selection - WITH BUSINESS RULES
        steps.add(
            SharedSteps.marineUnitSelectionStep(
                units = marineUnits,
                allowMultipleSelection = releaseMortgageRules.allowMultipleSelection(),
                showOwnedUnitsWarning = true
            )
        )

        // Step 4: Upload Documents (Dynamic from API)
        println("🔍 DEBUG: requiredDocuments.size = ${requiredDocuments.size}")
        steps.add(
            SharedSteps.dynamicDocumentsStep(
                documents = requiredDocuments  // ✅ Pass documents from API
            )
        )

        // Step 5: Review
        steps.add(SharedSteps.reviewStep())

        val hasRequestId = accumulatedFormData["requestId"] != null

        if (hasRequestId) {
            // Payment Details Step - Shows payment breakdown
            steps.add(SharedSteps.paymentDetailsStep(accumulatedFormData))
        }

        return steps
    }

    /**
     * Called when a step is opened - triggers payment API call when payment step is opened
     */
    override suspend fun onStepOpened(stepIndex: Int) {
        val step = getSteps().getOrNull(stepIndex) ?: return

        // ✅ If this is the payment step, trigger payment API call
        if (step.stepType == StepType.PAYMENT) {
            println("💰 Payment step opened - triggering payment receipt API call...")

            // Call PaymentManager to load payment receipt
            val paymentResult = paymentManager.processStepIfNeeded(
                stepType = StepType.PAYMENT,
                formData = accumulatedFormData,
                requestTypeId = TransactionType.RELEASE_MORTGAGE.toRequestTypeId().toInt(),
                context = transactionContext
            )

            when (paymentResult) {
                is StepProcessResult.Success -> {
                    println("✅ Payment receipt loaded - triggering step rebuild")
                    onStepsNeedRebuild?.invoke()
                }
                is StepProcessResult.Error -> {
                    println("❌ Payment error: ${paymentResult.message}")
                    accumulatedFormData["apiError"] = paymentResult.message
                }
                is StepProcessResult.NoAction -> {
                    println("ℹ️ No payment action needed")
                }
            }
            return // Don't process other logic for payment step
        }
    }

    // NEW: Validate marine unit selection with business rules
    suspend fun validateMarineUnitSelection(unitId: String, userId: String): ValidationResult {
        val unit = marineUnits.find { it.id.toString() == unitId }
            ?: return ValidationResult.Error("الوحدة البحرية غير موجودة")

        val (validationResult, navigationAction) = validateMarineUnitUseCase.executeAndGetAction(
            unit = unit,
            userId = userId,
            rules = releaseMortgageRules
        )

        return ValidationResult.Success(validationResult, navigationAction)
    }

    // NEW: Get only eligible units (mortgaged units) for this transaction
    suspend fun getEligibleMarineUnits(userId: String): List<MarineUnit> {
        return getEligibleUnitsUseCase.getEligibleOnly(userId, releaseMortgageRules)
    }

    override fun validateStep(step: Int, data: Map<String, Any>): Pair<Boolean, Map<String, String>> {
        val stepData = getSteps().getOrNull(step) ?: return Pair(false, emptyMap())
        val formData = data.mapValues { it.value.toString() }
        return validationUseCase.validateStep(stepData, formData)
    }

    override suspend fun processStepData(step: Int, data: Map<String, String>): Int {
        // ✅ Accumulate form data for dynamic step logic
        accumulatedFormData.putAll(data)
        println("📦 ReleaseMortgage - Accumulated data: $accumulatedFormData")

        // ✅ Check current step
        val steps = getSteps()
        val currentStepData = steps.getOrNull(step)
        val stepType = currentStepData?.stepType

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
                        createdRedemptionRequestId = result.requestId
                        accumulatedFormData["createdRequestId"] = result.requestId.toString()
                        accumulatedFormData["requestId"] = result.requestId.toString()

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
                                    println("✅ Stored coreShipsInfoId for payment: $firstShipId")
                                }
                            } catch (e: Exception) {
                                println("⚠️ Failed to extract shipInfoId: ${e.message}")
                            }
                        }
                    }
                    is com.informatique.mtcit.business.transactions.shared.ShipSelectionResult.Error -> {
                        println("❌ Ship selection failed: ${result.message}")
                        accumulatedFormData["apiError"] = result.message
                        // ✅ Throw exception to trigger error banner display
                        throw ApiException(500, result.message)
                    }
                }
            } catch (e: ApiException) {
                println("❌ ApiException in ship selection: ${e.message}")
                accumulatedFormData["apiError"] = e.message ?: "Unknown error"
                throw e // Re-throw to show error banner
            } catch (e: Exception) {
                println("❌ Exception in ship selection: ${e.message}")
                val errorMsg = com.informatique.mtcit.common.ErrorMessageExtractor.extract(e.message)
                accumulatedFormData["apiError"] = errorMsg
                throw ApiException(500, errorMsg)
            }
        }

        // Check if the current step has any document upload fields
        val hasFileUpload = currentStepData?.fields?.any { field ->
            field is FormField.FileUpload
        } == true

        if (hasFileUpload && requiredDocuments.isNotEmpty()) {
            // Check if at least one document has been uploaded
            val hasUploadedDocument = requiredDocuments.any { docItem ->
                val fieldId = "document_${docItem.document.id}"
                accumulatedFormData[fieldId]?.startsWith("content://") == true
            }

            if (hasUploadedDocument) {
                println("📤 File upload step completed - creating redemption request NOW")

                // Create the redemption request immediately
                try {
                    val result = createRedemptionRequest(accumulatedFormData)

                    result.onSuccess { response ->
                        // ✅ Store the request ID for later status update in review step
                        createdRedemptionRequestId = response.data.id
                        println("💾 STORED REDEMPTION REQUEST ID: $createdRedemptionRequestId")

                        // Store in formData as well
                        accumulatedFormData["requestId"] = response.data.id.toString()
                    }

                    result.onFailure { error ->
                        println("❌ Failed to create redemption request: ${error.message}")
                        val msg = when (error) {
                            is ApiException -> error.message ?: "فشل في إنشاء طلب فك الرهن"
                            else -> ErrorMessageExtractor.extract(error.message)
                        }
                        accumulatedFormData["apiError"] = msg
                        lastApiError = msg
                        if (error is ApiException) throw error else throw ApiException(500, msg)
                    }
                } catch (e: Exception) {
                    println("❌ Exception in createRedemptionRequest: ${e.message}")
                    e.printStackTrace()

                    // ✅ Provide helpful error message in Arabic
                    val userMessage = when {
                        e.message?.contains("400") == true ->
                            "خطأ في البيانات المرسلة إلى الخادم (400). يرجى التأكد من:\n" +
                            "• اختيار سفينة صحيحة\n" +
                            "• رفع جميع المستندات الإلزامية\n" +
                            "• الاتصال بالإنترنت"

                        e.message?.contains("404") == true ->
                            "لم يتم العثور على خدمة فك الرهن على الخادم (404)"

                        e.message?.contains("500") == true ->
                            "خطأ في الخادم (500). يرجى المحاولة لاحقاً"

                        e.message?.contains("timeout") == true || e.message?.contains("Timeout") == true ->
                            "انتهت مهلة الاتصال. يرجى التحقق من الإنترنت والمحاولة مجدداً"

                        else ->
                            "فشل إنشاء طلب فك الرهن: ${e.message}"
                    }

                    // Re-throw with user-friendly message
                    throw Exception(userMessage)
                }
            }
        }

        // ✅ HANDLE REVIEW STEP - Use ReviewManager
        if (currentStepData?.stepType == StepType.REVIEW) {
            println("📋 Handling Review Step using ReviewManager for Release Mortgage")

            val requestIdInt = createdRedemptionRequestId ?: accumulatedFormData["redemptionRequestId"]?.toIntOrNull()
            if (requestIdInt == null) {
                println("❌ No redemptionRequestId available for review step")
                accumulatedFormData["apiError"] = "لم يتم العثور على رقم طلب فك الرهن"
                return -1
            }

            try {
                // ✅ Get endpoint and context from transactionContext
                val transactionContext = TransactionType.RELEASE_MORTGAGE.context
                val endpoint = transactionContext.sendRequestEndpoint.replace("{requestId}", requestIdInt.toString())
                val contextName = transactionContext.displayName

                println("🚀 Calling ReviewManager.processReviewStep:")
                println("   Endpoint: $endpoint")
                println("   RequestId: $requestIdInt")
                println("   Context: $contextName")

                // ✅ Call ReviewManager which internally uses marineUnitsApiService via repository
                val reviewResult = reviewManager.processReviewStep(
                    endpoint = endpoint,
                    requestId = requestIdInt,
                    transactionName = contextName,
                    sendRequestPostOrPut = transactionContext.sendRequestPostOrPut
                )

                when (reviewResult) {
                    is com.informatique.mtcit.business.transactions.shared.ReviewResult.Success -> {
                        println("✅ Review step processed successfully!")
                        println("   Message: ${reviewResult.message}")
                        println("   Need Inspection: ${reviewResult.needInspection}")
                        println("   Has Acceptance: ${reviewResult.hasAcceptance}")

                        // ✅ Store response in formData
                        accumulatedFormData["sendRequestMessage"] = reviewResult.message
                        accumulatedFormData["hasAcceptance"] = reviewResult.hasAcceptance.toString()

                        // ✅ Extract coreShipsInfoId from the response for payment
                        // The structure is: data.shipInfo.id
                        println("🔍 Extracting coreShipsInfoId from response...")
                        println("   additionalData keys: ${reviewResult.additionalData?.keys}")

                        val coreShipsInfoId = reviewResult.additionalData?.get("shipInfo")?.let { shipInfo ->
                            println("   shipInfo type: ${shipInfo?.javaClass?.simpleName}")
                            val shipInfoMap = shipInfo as? Map<*, *>
                            println("   shipInfo keys: ${shipInfoMap?.keys}")
                            val id = shipInfoMap?.get("id")
                            println("   id value: $id, type: ${id?.javaClass?.simpleName}")

                            // Convert to String - handle both Int and String
                            when (id) {
                                is Number -> id.toString()
                                is String -> id
                                else -> id?.toString()
                            }
                        }

                        if (coreShipsInfoId != null) {
                            accumulatedFormData["coreShipsInfoId"] = coreShipsInfoId
                            println("✅ Extracted coreShipsInfoId for payment: $coreShipsInfoId")
                        } else {
                            println("⚠️ Could not extract coreShipsInfoId from response")
                            println("⚠️ Full additionalData: ${reviewResult.additionalData}")
                        }

                        // ✅ Extract request number
                        val requestNumber = reviewResult.additionalData?.get("requestNumber")?.toString()
                            ?: reviewResult.additionalData?.get("requestSerial")?.toString()
                            ?: accumulatedFormData["requestSerial"]
                            ?: "N/A"

                        // ✅ NEW: Check if this is a NEW request
                        val isNewRequest = accumulatedFormData["requestId"] == null ||
                                          accumulatedFormData["isResumedTransaction"]?.toBoolean() != true

                        // ✅ Use hasAcceptance from strategy property (set from TransactionDetail API), not from review response
                        val strategyHasAcceptance = this.hasAcceptance

                        println("🔍 Post-submission flow decision:")
                        println("   - isNewRequest: $isNewRequest")
                        println("   - hasAcceptance (from strategy): $strategyHasAcceptance")
                        println("   - hasAcceptance (from review API): ${reviewResult.hasAcceptance}")

                        // ✅ Only stop if BOTH isNewRequest AND hasAcceptance are true
                        if (isNewRequest && strategyHasAcceptance) {
                            println("🎉 NEW release mortgage request submitted with hasAcceptance=true - showing success dialog and stopping")
                            println("   User must continue from profile screen")

                            // Set success flags for ViewModel to show dialog
                            accumulatedFormData["requestSubmitted"] = "true"
                            accumulatedFormData["requestNumber"] = requestNumber
                            accumulatedFormData["successMessage"] = reviewResult.message

                            // Return -2 to indicate: success but show dialog and stop
                            return -2
                        } else if (isNewRequest && !strategyHasAcceptance) {
                            println("✅ NEW release mortgage request submitted with hasAcceptance=false - continuing to next steps")
                            println("   Transaction will continue to payment/next steps")
                            // Continue normally - don't return, let the flow proceed
                        } else {
                            println("✅ Resumed request - release mortgage request submitted successfully")
                        }
                    }
                    is com.informatique.mtcit.business.transactions.shared.ReviewResult.Error -> {
                        println("❌ Review step failed: ${reviewResult.message}")
                        accumulatedFormData["apiError"] = reviewResult.message
                        return -1 // Block navigation
                    }
                }
            } catch (e: com.informatique.mtcit.common.ApiException) {
                // ✅ Re-throw ApiException (e.g. 401) so ViewModel can show refresh button
                println("❌ ApiException in review step: ${e.code} - ${e.message}")
                throw e
            } catch (e: Exception) {
                println("❌ Exception in review step: ${e.message}")
                e.printStackTrace()
                accumulatedFormData["apiError"] = "حدث خطأ أثناء إرسال الطلب: ${e.message}"
                return -1
            }
        }

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
     * Create redemption request with the accumulated form data
     * This is called automatically after file upload step
     * ✅ UPDATED: Now uses createRedemptionRequestWithDocuments with documents array
     */
    private suspend fun createRedemptionRequest(formData: Map<String, String>): Result<com.informatique.mtcit.data.model.CreateRedemptionResponse> {
        println("=".repeat(80))
        println("🔓 Creating mortgage redemption request...")
        println("=".repeat(80))

        // Extract data from form
        val selectedUnitsJson = formData["selectedMarineUnits"]

        println("📋 Form Data:")
        println("   Selected Units JSON: $selectedUnitsJson")

        // Validate required fields
        if (selectedUnitsJson.isNullOrBlank() || selectedUnitsJson == "[]") {
            println("❌ Marine unit not selected")
            return Result.failure(Exception("يرجى اختيار السفينة"))
        }

        // Parse the selected ship ID
        val shipId = try {
            val cleanJson = selectedUnitsJson.trim().removeSurrounding("[", "]")
            val shipIds = cleanJson.split(",").map { it.trim().removeSurrounding("\"") }
            val firstShipId = shipIds.firstOrNull()

            if (firstShipId.isNullOrBlank()) {
                println("❌ Failed to parse ship ID from: $selectedUnitsJson")
                return Result.failure(Exception("تنسيق اختيار السفينة غير صالح"))
            }

            println("📍 Extracted ship ID: $firstShipId")

            // ✅ FIXED: The JSON contains shipInfoId directly, not maritimeId
            // Try to convert to Int directly
            val actualShipId = firstShipId.toIntOrNull()
            if (actualShipId == null) {
                println("❌ Ship ID is not a valid integer: $firstShipId")
                return Result.failure(Exception("معرف السفينة غير صالح"))
            }

            // ✅ Optional: Find the MarineUnit for logging (not required for API call)
            val selectedUnit = marineUnits.firstOrNull { it.id == firstShipId }
            if (selectedUnit != null) {
                println("✅ Found matching MarineUnit:")
                println("   Ship ID: $actualShipId")
                println("   Ship Name: ${selectedUnit.shipName}")
                println("   Maritime ID (MMSI): ${selectedUnit.maritimeId}")
            } else {
                println("⚠️ MarineUnit not found in cache, but using shipId: $actualShipId")
            }

            actualShipId
        } catch (e: Exception) {
            println("❌ Exception parsing selected units: ${e.message}")
            e.printStackTrace()
            return Result.failure(Exception("فشل تحليل السفينة المحددة: ${e.message}"))
        }

        // ✅ NEW: Collect all uploaded documents from dynamic fields
        val uploadedDocuments = mutableListOf<OwnerFileUpload>()

        // Get all document fields (document_101, document_102, etc.)
        requiredDocuments
            .filter { it.document.isActive == 1 }
            .forEach { docItem ->
                val fieldId = "document_${docItem.document.id}"
                val documentUri = formData[fieldId]

                println("🔍 Checking document field: $fieldId = $documentUri")

                if (!documentUri.isNullOrBlank() && documentUri.startsWith("content://")) {
                    try {
                        val uri = android.net.Uri.parse(documentUri)
                        val fileUpload = FileUploadHelper.uriToFileUpload(appContext, uri)

                        if (fileUpload != null) {
                            // Determine proper MIME type
                            val properMimeType = when {
                                fileUpload.fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                                fileUpload.fileName.endsWith(".jpg", ignoreCase = true) ||
                                fileUpload.fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                                fileUpload.fileName.endsWith(".png", ignoreCase = true) -> "image/png"
                                fileUpload.fileName.endsWith(".doc", ignoreCase = true) -> "application/msword"
                                fileUpload.fileName.endsWith(".docx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                else -> fileUpload.mimeType
                            }

                            val ownerFile = OwnerFileUpload(
                                fileName = fileUpload.fileName,
                                fileUri = fileUpload.fileUri,
                                fileBytes = fileUpload.fileBytes,
                                mimeType = properMimeType,
                                docOwnerId = "document_${docItem.document.id}",
                                docId = docItem.document.id // ✅ Send the actual document ID from API
                            )

                            uploadedDocuments.add(ownerFile)
                            println("📎 Added document: ${docItem.document.nameAr} (id=${docItem.document.id}, file=${ownerFile.fileName}, mimeType=$properMimeType)")
                        }
                    } catch (e: Exception) {
                        println("⚠️ Failed to process document ${docItem.document.nameAr}: ${e.message}")
                    }
                } else {
                    // Check if document is mandatory
                    if (docItem.document.isMandatory == 1) {
                        println("❌ Mandatory document missing: ${docItem.document.nameAr}")
                        return Result.failure(Exception("المستند '${docItem.document.nameAr}' إلزامي ولم يتم رفعه"))
                    }
                }
            }

        println("📋 Total documents to upload: ${uploadedDocuments.size}")

        // Validate that at least one document is uploaded
        if (uploadedDocuments.isEmpty()) {
            println("❌ No documents uploaded")
            return Result.failure(Exception("يرجى رفع المستندات المطلوبة"))
        }

        // ✅ Create the redemption request with documents array
        val request = com.informatique.mtcit.data.model.CreateRedemptionRequest(
            shipInfoId = shipId,
            documents = uploadedDocuments.map { doc ->
                com.informatique.mtcit.data.model.RedemptionDocumentRef(
                    fileName = doc.fileName,
                    documentId = doc.docId
                )
            }
        )

        println("📤 Sending redemption request to API...")
        println("   Ship ID: ${request.shipInfoId}")
        println("   Documents: ${request.documents?.size ?: 0}")
        request.documents?.forEach { doc ->
            println("      - ${doc.fileName} (documentId=${doc.documentId})")
        }

        // ✅ Call new API with documents
        val result = mortgageApiService.createRedemptionRequestWithDocuments(request, uploadedDocuments)

        result.onSuccess { response ->
            createdRedemptionRequestId = response.data.id
            println("=".repeat(80))
            println("💾 STORED REDEMPTION REQUEST ID: $createdRedemptionRequestId")
            println("=".repeat(80))

            if (uploadedDocuments.isNotEmpty()) {
                println("✅ Uploaded documents:")
                uploadedDocuments.forEach { doc ->
                    println("   - ${doc.fileName} (docId=${doc.docId})")
                }
            }
        }

        result.onFailure { error ->
            println("❌ Failed to create redemption request: ${error.message}")
            val msg = when (error) {
                is ApiException -> error.message ?: "فشل في إنشاء طلب فك الرهن"
                else -> ErrorMessageExtractor.extract(error.message)
            }
            accumulatedFormData["apiError"] = msg
            lastApiError = msg
            if (error is ApiException) throw error else throw ApiException(500, msg)
        }

        println("=".repeat(80))

        return result
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        println("=".repeat(80))
        println("📤 ReleaseMortgageStrategy.submit() called")
        println("=".repeat(80))

        // ✅ Get the created request ID
        val requestId = createdRedemptionRequestId

        if (requestId == null) {
            println("❌ No redemption request ID found - cannot submit")
            return Result.failure(Exception("لم يتم العثور على رقم الطلب. يرجى المحاولة مرة أخرى."))
        }

        println("✅ Redemption Request ID: $requestId")
        println("✅ Strategy validation complete - ready for submission")
        println("   ViewModel will handle API call via submitOnReview()")
        println("=".repeat(80))

        // ✅ Return success - ViewModel will call submitOnReview() which handles the API
        // No direct API call here - keep Strategy focused on business logic only
        return Result.success(true)
    }

    // ✅ Implement interface methods for request ID and status update
    override fun getCreatedRequestId(): Int? {
        return createdRedemptionRequestId
    }

    override fun getStatusUpdateEndpoint(requestId: Int): String? {
        return context.buildUpdateStatusUrl(requestId)
    }

    override fun getSendRequestEndpoint(requestId: Int): String {
        return context.buildSendRequestUrl(requestId)
    }

    override fun getTransactionTypeName(): String {
        return "فك الرهن"
    }

    /**
     * ✅ Get the transaction context with all API endpoints
     */
    override fun getContext(): TransactionContext {
        return TransactionType.RELEASE_MORTGAGE.context
    }

    /**
     * ✅ Store API response for future actions
     */
    override fun storeApiResponse(apiName: String, response: Any) {
        println("💾 Storing API response for '$apiName': $response")
        apiResponses[apiName] = response
    }

    /**
     * ✅ Get stored API response
     */
    override fun getApiResponse(apiName: String): Any? {
        return apiResponses[apiName]
    }

    /**
     * ✅ DRAFT TRACKING: Extract completed steps from API response
     * Used when resuming a draft request to initialize posted steps
     */
    override fun extractCompletedStepsFromApiResponse(response: Any): Set<StepType> {
        println("🔍 ReleaseMortgageStrategy: Extracting completed steps from API response")

        val completedSteps = mutableSetOf<StepType>()

        try {
            // Release Mortgage has minimal steps:
            // 1. PERSON_TYPE (always completed if request exists)
            // 2. MARINE_UNIT_SELECTION (if mortgaged ship selected)
            // 3. DOCUMENTS (if documents uploaded)
            // 4. REVIEW (if request sent)

            if (response is Map<*, *>) {
                val data = response["data"] as? Map<*, *>

                if (data != null) {
                    // ✅ PERSON_TYPE always completed if request exists
                    completedSteps.add(StepType.PERSON_TYPE)
                    println("   ✅ PERSON_TYPE completed")

                    // ✅ MARINE_UNIT_SELECTION if shipInfo exists
                    val shipInfo = data["shipInfo"]
                    if (shipInfo != null) {
                        completedSteps.add(StepType.MARINE_UNIT_SELECTION)
                        println("   ✅ MARINE_UNIT_SELECTION completed")
                    }

                    // ✅ DOCUMENTS if documents array exists and not empty
                    val documents = data["documents"] as? List<*>
                    if (documents != null && documents.isNotEmpty()) {
                        completedSteps.add(StepType.DOCUMENTS)
                        println("   ✅ DOCUMENTS completed (${documents.size} document(s))")
                    }

                    // ✅ REVIEW if status is not DRAFT
                    val status = data["status"] as? Map<*, *>
                    val statusId = (status?.get("id") as? Number)?.toInt()
                    if (statusId != null && statusId != 1) { // 1 = DRAFT
                        completedSteps.add(StepType.REVIEW)
                        println("   ✅ REVIEW completed (status: $statusId)")
                    }
                }
            }

            println("📊 Total completed steps: ${completedSteps.size}")
            return completedSteps

        } catch (e: Exception) {
            println("⚠️ Error extracting completed steps: ${e.message}")
            e.printStackTrace()
            return emptySet()
        }
    }
}
