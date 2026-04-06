package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.transactions.marineunit.rules.TemporaryRegistrationRules
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.business.transactions.shared.StepProcessResult
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.validation.rules.FormatValidationRules
import com.informatique.mtcit.business.validation.rules.ValidationRule
import com.informatique.mtcit.common.ApiException
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.data.repository.MarineUnitRepository
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.ui.components.PersonType
import com.informatique.mtcit.ui.components.SelectableItem
import com.informatique.mtcit.ui.repo.CompanyRepo
import com.informatique.mtcit.ui.viewmodels.StepData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.informatique.mtcit.R
import com.informatique.mtcit.business.transactions.shared.Certificate
import com.informatique.mtcit.business.transactions.shared.StepType
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.data.model.RequiredDocumentItem

import com.informatique.mtcit.ui.components.DropdownSection
import dagger.hilt.android.qualifiers.ApplicationContext
import com.informatique.mtcit.util.UserHelper
import com.informatique.mtcit.common.util.AppLanguage

class ChangeNameOfShipOrUnitStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val companyRepository: CompanyRepo,
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository,
    private val marineUnitRepository: MarineUnitRepository,
    private val temporaryRegistrationRules: TemporaryRegistrationRules,
    private val shipSelectionManager: com.informatique.mtcit.business.transactions.shared.ShipSelectionManager,
    private val reviewManager: com.informatique.mtcit.business.transactions.shared.ReviewManager,
    private val paymentManager: com.informatique.mtcit.business.transactions.shared.PaymentManager,
    private val inspectionFlowManager: com.informatique.mtcit.business.transactions.shared.InspectionFlowManager,
    @ApplicationContext private val appContext: Context
) : BaseTransactionStrategy() {
    private val transactionContext: TransactionContext = TransactionType.SHIP_NAME_CHANGE.context
    private var portOptions: List<String> = emptyList()
    private var countryOptions: List<String> = emptyList()
    private var shipTypeOptions: List<String> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()
    private var commercialOptions: List<SelectableItem> = emptyList()
    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf()
    private var loadedCertificates = (mutableStateListOf<Certificate>())
    private var typeOptions: List<PersonType> = emptyList()
    private val requestTypeId = TransactionType.SHIP_NAME_CHANGE.toRequestTypeId()

    // ✅ INFINITE SCROLL: pagination state
    private var _currentShipsPage: Int = -1
    private var _isLastShipsPage: Boolean = true
    override val currentShipsPage: Int get() = _currentShipsPage
    override val isLastShipsPage: Boolean get() = _isLastShipsPage
    // Store API error to prevent navigation and show error dialog
    private var lastApiError: String? = null

    // ✅ NEW: Store created request ID for status update
    private var createdRequestId: Int? = null

    // ✅ NEW: Store API responses for future actions
    private val apiResponses: MutableMap<String, Any> = mutableMapOf()

    // ✅ Store shipInfoId for affected certificates and other APIs
    private var selectedShipInfoId: Int? = null

    // ✅ Store current port from selected ship (port name, not object)
    private var currentPortOfRegistry: String? = null

    // ✅ NEW: Store loaded inspection authorities
    private var loadedInspectionAuthorities: List<DropdownSection> = emptyList()

    // ✅ NEW: Store inspection-specific documents (separate from permanent registration documents)
    private var loadedInspectionDocuments: List<RequiredDocumentItem> = emptyList()


    /**
     * ✅ Override setHasAcceptanceFromApi to also store in formData
     * This ensures the payment success dialog can access it
     */
    override fun setHasAcceptanceFromApi(hasAcceptanceValue: Int?) {
        super.setHasAcceptanceFromApi(hasAcceptanceValue)
        // ✅ Store in formData so PaymentSuccessDialog can access it
        accumulatedFormData["hasAcceptance"] = (hasAcceptanceValue == 1).toString()
        println("🔧 ChangeNameOfShipOrUnitStrategy: Stored hasAcceptance in formData: ${accumulatedFormData["hasAcceptance"]}")
    }

    /**
     * Handle inspection dialog confirmation
     * Called when user clicks "Continue" on inspection required dialog
     * This will load inspection lookups and inject the inspection purpose step
     */
    suspend fun handleInspectionContinue() {
        println("🔍 ChangeNameOfShipOrUnitStrategy: User confirmed inspection requirement")
        println("   Loading inspection lookups...")

        try {
            // Get shipInfoId from accumulatedFormData
            val shipInfoIdStr = accumulatedFormData["coreShipsInfoId"]
                ?: accumulatedFormData["shipInfoId"]
                ?: run {
                    println("❌ No shipInfoId found in formData")
                    accumulatedFormData["apiError"] = if (AppLanguage.isArabic) "لم يتم العثور على معرف السفينة" else "Ship ID not found"
                    return
                }

            val shipInfoId = shipInfoIdStr.toIntOrNull() ?: run {
                println("❌ Invalid shipInfoId: $shipInfoIdStr")
                accumulatedFormData["apiError"] = if (AppLanguage.isArabic) "معرف السفينة غير صالح" else "Invalid ship ID"
                return
            }

            println("   Using shipInfoId: $shipInfoId")

            // Load inspection lookups (purposes, places, authorities)
            val lookups = inspectionFlowManager.loadInspectionLookups(shipInfoId)

            println("✅ Inspection lookups loaded:")
            println("   - Purposes: ${lookups.purposes.size}")
            println("   - Places: ${lookups.places.size}")
            println("   - Authority sections: ${lookups.authoritySections.size}")
            println("   - Documents: ${lookups.documents.size}") // ✅ Log documents

            // ✅ CRITICAL: Store authorities AND documents in member variables BEFORE setting showInspectionStep
            loadedInspectionAuthorities = lookups.authoritySections
            loadedInspectionDocuments = lookups.documents // ✅ Store inspection documents

            // Mark that inspection step should be shown
            accumulatedFormData["showInspectionStep"] = "true"
            accumulatedFormData["inspectionPurposes"] = lookups.purposes.joinToString(",")
            accumulatedFormData["inspectionPlaces"] = lookups.places.joinToString(",")

            // Clear dialog flag
            accumulatedFormData.remove("showInspectionDialog")

            println("✅ Inspection lookups loaded, triggering steps rebuild")

            // Trigger steps rebuild to inject inspection step
            onStepsNeedRebuild?.invoke()

        } catch (e: Exception) {
            println("❌ Failed to load inspection lookups: ${e.message}")
            e.printStackTrace()
            accumulatedFormData["apiError"] = if (AppLanguage.isArabic) "فشل تحميل بيانات المعاينة: ${e.message}" else "Failed to load inspection data: ${e.message}"
        }
    }

    /**
     * Handle inspection dialog cancel
     * Called when user clicks "Cancel" on inspection required dialog
     */
    fun handleInspectionCancel() {
        println("ℹ️ ChangeNameOfShipOrUnitStrategy: User cancelled inspection requirement")

        // Just clear the dialog flag and stay on review step
        accumulatedFormData.remove("showInspectionDialog")

        // Set flag to show that request is sent but pending inspection
        accumulatedFormData["requestPendingInspection"] = "true"
    }

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        // ✅ Get civilId from token
        val ownerCivilId = UserHelper.getOwnerCivilId(appContext)
        println("🔑 Owner CivilId from token: $ownerCivilId")

        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val shipTypes = lookupRepository.getShipTypes().getOrNull() ?: emptyList()

        // ✅ Handle null civilId - return empty list if no token
        val commercialRegistrations = if (ownerCivilId != null) {
            lookupRepository.getCommercialRegistrations(ownerCivilId).getOrNull() ?: emptyList()
        } else {
            emptyList()
        }
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()

        portOptions = ports
        countryOptions = countries
        shipTypeOptions = shipTypes
        commercialOptions = commercialRegistrations
        typeOptions = personTypes

        // ✅ Don't load ships here - they will be loaded when user presses Next
        // after selecting person type (individual/company)
        println("🚢 Skipping initial ship load - will load after user selects type and presses Next")

        return mapOf(
            "marineUnits" to emptyList<MarineUnit>(), // ✅ Empty initially
            "commercialRegistration" to commercialRegistrations,
            "personType" to personTypes,
            "certificates" to loadedCertificates
        )
    }

    /**
     * ✅ NEW: Load ships when user selects type and presses Next
     */
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

        println("🔍 Calling loadShipsForOwner with ownerCivilId=$ownerCivilId, commercialRegNumber=$commercialRegNumber")

        println("🔍 Loading first page with loadShipsPage(page=0)")
        val firstPage = marineUnitRepository.loadShipsPage(
            ownerCivilId = ownerCivilId,
            commercialRegNumber = commercialRegNumber,
            requestTypeId = requestTypeId, // ✅ "11" from TransactionType.SHIP_NAME_CHANGE
            page = 0
        )
        marineUnits = firstPage.ships
        _currentShipsPage = 0
        _isLastShipsPage = firstPage.isLastPage

        println("✅ Loaded ${marineUnits.size} ships (isLast=$_isLastShipsPage)")
        marineUnits.forEach { unit -> println("   - ${unit.shipName} (ID: ${unit.id})") }
        return marineUnits
    }

    /**
     * ✅ NEW: Clear loaded ships when user goes back
     */
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
        println("📄 loadNextShipsPage (ChangeName) page=$nextPage")
        val result = marineUnitRepository.loadShipsPage(
            ownerCivilId = ownerCivilId,
            commercialRegNumber = commercialReg,
            requestTypeId = requestTypeId, // ✅ "11" from TransactionType.SHIP_NAME_CHANGE
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
        println("📦 ChangeShipName - Updated accumulated data: $accumulatedFormData")
    }

    override fun getContext(): TransactionContext {
        return transactionContext
    }

    override fun getSteps(): List<StepData> {
        val steps = mutableListOf<StepData>()

        // Step 1: Person Type
        steps.add(SharedSteps.personTypeStep(typeOptions))

        // Step 2: Commercial Registration (فقط للشركات)
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

        // Step 4: Change Name Information
        steps.add(
            StepData(
                titleRes = R.string.change_name_of_ship_or_unit_strategy_info,
                descriptionRes = R.string.change_name_of_ship_or_unit_strategy_info_desc,
                stepType = StepType.MARINE_UNIT_NAME_SELECTION,
                fields = listOf(
                    FormField.CurrentValueCard(
                        id = "current_ship_name",
                        labelRes = R.string.previous_ship_name
                    ),
                    FormField.TextField(
                        id = "new_ship_name_ar",
                        labelRes = R.string.marine_unit_name_ar,
                        placeholder = R.string.marine_unit_name_ar_placeholder.toString(),
                        mandatory = true,
                        minLength = 3, // ✅ الحد الأدنى 3 أحرف
                        maxLength = 50 // ✅ الحد الأقصى 50 حرف
                    ),
                    FormField.TextField(
                        id = "new_ship_name_en",
                        labelRes = R.string.marine_unit_name_en,
                        placeholder = R.string.marine_unit_name_en_placeholder.toString(),
                        mandatory = true,
                        enabled = true,
                        minLength = 3, // ✅ الحد الأدنى 3 أحرف
                        maxLength = 50 // ✅ الحد الأقصى 50 حرف
                    )
                ),
                requiredLookups = emptyList()
            )
        )

        // Step 5: Affected Certificates (loaded from API)
        steps.add(SharedSteps.createCertificateStep(
            certificates = loadedCertificates,
            titleRes = R.string.change_name_certificate_step_title,
            descriptionRes = R.string.change_name_certificate_step_desc
        ))

        // Step 6: Review Step
        steps.add(SharedSteps.reviewStep())

        // ✅ NEW: Inspection Purpose Step (dynamically added when inspection is required)
        val showInspectionStep = accumulatedFormData["showInspectionStep"]?.toBoolean() ?: false
        if (showInspectionStep) {
            println("📋 Adding Inspection Purpose Step (dynamically injected)")

            // Parse lookups from formData
            val purposes =
                accumulatedFormData["inspectionPurposes"]?.split(",")?.filter { it.isNotBlank() }
                    ?: emptyList()
            val places =
                accumulatedFormData["inspectionPlaces"]?.split(",")?.filter { it.isNotBlank() }
                    ?: emptyList()

            println("   - Purposes: ${purposes.size}")
            println("   - Places: ${places.size}")
            println("   - Authority sections: ${loadedInspectionAuthorities.size}")
            println("   - Inspection Documents: ${loadedInspectionDocuments.size}") // ✅ Log inspection documents

            // ✅ Use inspection-specific documents (NOT permanent registration documents)
            steps.add(
                SharedSteps.inspectionPurposeAndAuthorityStep(
                    inspectionPurposes = purposes,
                    inspectionPlaces = places,
                    authoritySections = loadedInspectionAuthorities, // ✅ Use loaded authorities
                    documents = loadedInspectionDocuments // ✅ Use inspection documents (not requiredDocuments)
                )
            )
        }

        // ✅ Payment Steps - Only show AFTER review/send-request succeeds AND no inspection pending
        val hasRequestId = accumulatedFormData["sendRequestMessage"] != null
        val inspectionRequired = accumulatedFormData["showInspectionDialog"]?.toBoolean() ?: false

        println("🔍 Payment step visibility check:")
        println("   reviewSucceeded: $hasRequestId")
        println("   inspectionRequired: $inspectionRequired")
        println("   showInspectionStep: $showInspectionStep")

        // ✅ Only show payment steps AFTER review succeeds AND no inspection is pending
        if (hasRequestId && !inspectionRequired && !showInspectionStep) {
            println("✅ Adding payment steps")
            // Payment Details Step - Shows payment breakdown
            steps.add(SharedSteps.paymentDetailsStep(accumulatedFormData))

            // Payment Success Step - Only show if payment was successful
            val paymentSuccessful = accumulatedFormData["paymentSuccessful"]?.toBoolean() ?: false
            if (paymentSuccessful) {
                steps.add(SharedSteps.paymentSuccessStep())
            }
        } else {
            println("⏭️ Skipping payment steps (inspection required or in progress)")
        }

        println("📋 Total steps count: ${steps.size}")
        return steps
    }

    override fun validateStep(step: Int, data: Map<String, Any>): Pair<Boolean, Map<String, String>> {
        val stepData = getSteps().getOrNull(step) ?: return Pair(false, emptyMap())
        val formData = data.mapValues { it.value.toString() }

        // ✅ Get validation rules for this step
        val rules = getValidationRulesForStep(step, stepData)

        // ✅ Use accumulated data for validation (enables cross-step validation)
        return validationUseCase.validateStepWithAccumulatedData(
            stepData = stepData,
            currentStepData = formData,
            allAccumulatedData = accumulatedFormData,
            crossFieldRules = rules
        )
    }

    /**
     * Get validation rules based on step content
     */
    private fun getValidationRulesForStep(stepIndex: Int, stepData: StepData): List<ValidationRule> {
        val fieldIds = stepData.fields.map { it.id }
        val rules = mutableListOf<ValidationRule>()

        if (fieldIds.contains("new_ship_name_ar")) {
            rules.add(FormatValidationRules.arabicNumbersDash("new_ship_name_ar"))
        }
        if (fieldIds.contains("new_ship_name_en")) {
            rules.add(FormatValidationRules.englishNumbersDash("new_ship_name_en"))
        }

        return rules
    }

    override suspend fun processStepData(step: Int, data: Map<String, String>): Int {
        println("🔄 processStepData called with: $data")

        // ✅ Update accumulated data
        accumulatedFormData.putAll(data)

        println("📦 accumulatedFormData after update: $accumulatedFormData")
        println("📦 Current step data: $data")
        println("📦 Accumulated data: $accumulatedFormData")

        val currentStepData = getSteps().getOrNull(step)
        val stepType = currentStepData?.stepType

        println("🔍 Current step titleRes: ${currentStepData?.titleRes}")
        println("🔍 Current step stepType: ${currentStepData?.stepType}")

        // ✅ Step 1: Marine Unit Selection (owned_ships)
        if (currentStepData?.titleRes == R.string.owned_ships) {
            println("🚢 ✅ Marine Unit Selection step completed - calling proceed-request API...")

            try {
                // Get the selected ship ID from the form data
                val selectedShipId = data["selectedMarineUnits"]

                // ✅ Use ShipSelectionManager to handle proceed-request API
                val result = shipSelectionManager.handleShipSelection(
                    shipId = selectedShipId,
                    context = transactionContext
                )

                when (result) {
                    is com.informatique.mtcit.business.transactions.shared.ShipSelectionResult.Success -> {
                        println("✅ Ship selection successful via Manager!")
                        println("   Request ID: ${result.requestId}")

                        // ✅ Store the created request ID
                        createdRequestId = result.requestId
                        accumulatedFormData["requestId"] = result.requestId.toString()
                        apiResponses["proceedRequest"] = result.response

                        // ✅ Extract and store shipInfoId + current port
                        val selectedUnitsJson = data["selectedMarineUnits"]
                        if (selectedUnitsJson != null) {
                            try {
                                val cleanJson = selectedUnitsJson.trim().removeSurrounding("[", "]")
                                val shipIds =
                                    cleanJson.split(",").map { it.trim().removeSurrounding("\"") }
                                val firstShipId = shipIds.firstOrNull()
                                if (firstShipId != null) {
                                    selectedShipInfoId = firstShipId.toIntOrNull()
                                    accumulatedFormData["shipInfoId"] = firstShipId
                                    accumulatedFormData["coreShipsInfoId"] = firstShipId
                                    println("✅ Stored shipInfoId: $firstShipId")

                                    // ✅ Extract current port from selected ship
                                    val selectedShip =
                                        marineUnits.firstOrNull { it.id == firstShipId }
                                    currentPortOfRegistry = selectedShip?.portOfRegistry?.id
                                    println("✅ Stored currentPortId: $currentPortOfRegistry")
                                }
                            } catch (e: Exception) {
                                println("⚠️ Failed to extract shipInfoId: ${e.message}")
                            }
                        }
                        println("💾 STORED createdRequestId = $createdRequestId")
                    }

                    is com.informatique.mtcit.business.transactions.shared.ShipSelectionResult.Error -> {
                        println("❌ Ship selection failed: ${result.message}")
                        lastApiError = result.message
                        throw ApiException(500, result.message)
                    }
                }
            } catch (e: ApiException) {
                println("❌ ApiException in ship selection: ${e.message}")
                lastApiError = e.message ?: "Unknown error"
                throw e
            } catch (e: Exception) {
                println("❌ Exception in ship selection: ${e.message}")
                val errorMsg =
                    com.informatique.mtcit.common.ErrorMessageExtractor.extract(e.message)
                lastApiError = errorMsg
                throw ApiException(500, errorMsg)
            }
        }

        // ✅ Step: Marine Unit Name Selection
        if (currentStepData?.stepType == StepType.MARINE_UNIT_NAME_SELECTION) {
            println("📝 ✅ Marine Unit Name Selection step completed - calling change-ship-info/name API...")

            try {
                val requestId = accumulatedFormData["requestId"]?.toIntOrNull()
                    ?: throw ApiException(400, "Missing request ID")

                val newNameAr = data["new_ship_name_ar"]
                    ?: throw ApiException(400, "Missing new Arabic name")
                val newNameEn = data["new_ship_name_en"]
                    ?: throw ApiException(400, "Missing new English name")

                println("📤 Calling API with requestId=$requestId, nameAr=$newNameAr, nameEn=$newNameEn")

                val response = repository.changeShipName(
                    requestId = requestId,
                    shipName = newNameAr,
                    shipNameEn = newNameEn
                )

                if (response.isSuccess) {
                    val changeNameResponse = response.getOrNull()
                    println("✅ Name change successful!")
                    println("   New Request ID: ${changeNameResponse?.newRequestId}")
                    println("   New Ship Info ID: ${changeNameResponse?.newShipInfoId}")

                    // ✅ Store the new request ID from API
                    changeNameResponse?.newRequestId?.let { newId ->
                        createdRequestId = newId
                        accumulatedFormData["requestId"] = newId.toString()
                        println("✅ Stored createdRequestId = $newId")
                    }

                    // ✅ Store new shipInfoId if provided
                    changeNameResponse?.newShipInfoId?.let { newShipInfoId ->
                        selectedShipInfoId = newShipInfoId
                        accumulatedFormData["coreShipsInfoId"] = newShipInfoId.toString()
                        println("✅ Updated coreShipsInfoId to: $newShipInfoId")
                    }

                    apiResponses["changeName"] = changeNameResponse ?: Any()
                } else {
                    val error = response.exceptionOrNull()?.message ?: if (AppLanguage.isArabic) "فشل تغيير اسم السفينة" else "Failed to change ship name"
                    println("❌ Name change failed: $error")
                    lastApiError = error
                    throw ApiException(500, error)
                }
            } catch (e: ApiException) {
                println("❌ ApiException in name change: ${e.message}")
                lastApiError = e.message ?: "Unknown error"
                throw e
            } catch (e: Exception) {
                println("❌ Exception in name change: ${e.message}")
                val errorMsg = com.informatique.mtcit.common.ErrorMessageExtractor.extract(e.message)
                lastApiError = errorMsg
                throw ApiException(500, errorMsg)
            }
        }

        // ✅ Step 3: Affected Certificates (loaded in onStepOpened, not here)
        // No action needed in processStepData - certificates loaded when step opens

        // ✅ Step 4: Review Step (send-request)
        if (stepType == StepType.REVIEW) {
            println("📝 ✅ Review step completed - using ReviewManager...")

            val requestIdInt = createdRequestId
                ?: throw ApiException(400, "Missing request ID for review step")

            val endpoint = transactionContext.sendRequestEndpoint
            val contextName = transactionContext.displayName

            println("🚀 Calling ReviewManager.processReviewStep:")
            println("   Endpoint: $endpoint")
            println("   RequestId: $requestIdInt")
            println("   Context: $contextName")

            try {
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

                        // ✅ Store response in formData
                        accumulatedFormData["sendRequestMessage"] = reviewResult.message

                        // ✅ Extract request number for display
                        val requestNumber =
                            reviewResult.additionalData?.get("requestNumber")?.toString()
                                ?: reviewResult.additionalData?.get("requestSerial")?.toString()
                                ?: accumulatedFormData["requestSerial"]
                                ?: createdRequestId?.toString()
                                ?: "N/A"

                        accumulatedFormData["requestNumber"] = requestNumber
                        println("   Request Number: $requestNumber")

                        // ✅ STEP 1: Get hasAcceptance from formData (set via setHasAcceptanceFromApi)
                        val hasAcceptanceFromFormData =
                            accumulatedFormData["hasAcceptance"]?.toBoolean() ?: false
                        println("   Has Acceptance (from formData): $hasAcceptanceFromFormData")

                        // ✅ STEP 2: Check hasAcceptance FIRST (before inspection)
                        if (hasAcceptanceFromFormData) {
                            println("🛑 hasAcceptance=1: Transaction requires acceptance/approval")
                            println("   Stopping transaction - user must continue from profile later")

                            // Store success message for dialog
                            accumulatedFormData["successMessage"] = reviewResult.message
                            accumulatedFormData["requestSubmitted"] = "true"

                            // Return -2 to indicate: success but stop transaction (show dialog)
                            return -2
                        }

                        // ✅ STEP 3: Check if inspection is required (from API response)
                        if (inspectionFlowManager.isInspectionRequired(reviewResult.needInspection)) {
                            println("🔍 Inspection is required - preparing dialog")
                            println("📋 Change name request was ALREADY submitted successfully")

                            // Prepare inspection dialog with parent transaction info
                            // Request Type: 11 = Change Ship Name
                            inspectionFlowManager.prepareInspectionDialog(
                                message = if (AppLanguage.isArabic) "تم إرسال طلب تغيير اسم السفينة بنجاح (رقم الطلب: $requestNumber).\n\nالسفينة تحتاج إلى معاينة لإكمال الإجراءات. يرجى الاستمرار لتقديم طلب معاينة." else "Ship name change request submitted successfully (Request No: $requestNumber).\n\nThe ship requires an inspection. Please continue to submit an inspection request.",
                                formData = accumulatedFormData,
                                parentRequestId = requestIdInt,
                                parentRequestType = 11  // Change Ship Name
                            )

                            println("⚠️ Inspection required - showing dialog and blocking proceed")
                            return step // Stay on current step to show dialog
                        }

                        // ✅ STEP 4: Check if this is a NEW request (not resumed)
                        val isNewRequest =
                            accumulatedFormData["isResumedTransaction"]?.toBoolean() != true

                        println("🔍 Post-submission flow decision:")
                        println("   - isResumedTransaction: ${accumulatedFormData["isResumedTransaction"]}")
                        println("   - isNewRequest: $isNewRequest")
                        println("   - needInspection: ${reviewResult.needInspection}")
                        println("   - hasAcceptance (from formData): $hasAcceptanceFromFormData")

                        if (isNewRequest && !reviewResult.needInspection) {
                            println("✅ NEW request submitted successfully - continuing to payment/next steps")
                            println("   Transaction will continue normally")
                            // Continue normally - don't return, let the flow proceed
                        } else if (!isNewRequest) {
                            println("✅ RESUMED request - continuing to next steps")
                            // Continue normally for resumed transactions
                        }

                        // Trigger UI rebuild
                        onStepsNeedRebuild?.invoke()
                    }

                    is com.informatique.mtcit.business.transactions.shared.ReviewResult.Error -> {
                        println("❌ Review step failed: ${reviewResult.message}")
                        lastApiError = reviewResult.message
                        throw ApiException(500, reviewResult.message)
                    }
                }
            } catch (e: ApiException) {
                // ✅ Re-throw ApiException (e.g. 401) so ViewModel can show refresh button
                println("❌ ApiException in review step: ${e.code} - ${e.message}")
                throw e
            } catch (e: Exception) {
                println("❌ Exception in review step: ${e.message}")
                e.printStackTrace()
                lastApiError = e.message ?: if (AppLanguage.isArabic) "حدث خطأ أثناء إرسال الطلب" else "An error occurred while submitting the request"
                throw ApiException(500, e.message ?: if (AppLanguage.isArabic) "حدث خطأ أثناء إرسال الطلب" else "An error occurred while submitting the request")
            }
        }

        if (currentStepData?.stepType == StepType.PAYMENT) {
            println("💰 Handling Payment Step using PaymentManager")

            val paymentResult = paymentManager.processStepIfNeeded(
                stepType = stepType,
                formData = accumulatedFormData,
                requestTypeId = requestTypeId.toInt(),
                context = transactionContext
            )

            when (paymentResult) {
                is StepProcessResult.Success -> {
                    println("✅ Payment step processed: ${paymentResult.message}")

                    // Trigger UI rebuild so payment details are shown (important for mortgage path)
                    onStepsNeedRebuild?.invoke()

                    // Check if payment was submitted successfully
                    val showPaymentSuccessDialog =
                        accumulatedFormData["showPaymentSuccessDialog"]?.toBoolean() ?: false
                    if (showPaymentSuccessDialog) {
                        println("✅ Payment submitted successfully - dialog will be shown")
                        return step
                    }
                }

                is StepProcessResult.Error -> {
                    println("❌ Payment step failed: ${paymentResult.message}")
                    lastApiError = paymentResult.message
                    return -1
                }

                is StepProcessResult.NoAction -> {
                    println("ℹ️ No payment action needed")
                }
            }
        }

        return step
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        return repository.submitRegistration(data)
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
            return FieldFocusResult.Error("companyRegistrationNumber", if (AppLanguage.isArabic) "رقم السجل التجاري يجب أن يكون أكثر من 3 أرقام" else "Commercial registration number must be more than 3 digits")
        }

        return try {
            val result = companyRepository.fetchCompanyLookup(registrationNumber)
                .flowOn(Dispatchers.IO)
                .catch { throw Exception(if (AppLanguage.isArabic) "حدث خطأ أثناء البحث عن الشركة: ${it.message}" else "An error occurred while searching for the company: ${it.message}") }
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
                is BusinessState.Error -> FieldFocusResult.Error("companyRegistrationNumber", result.message)
                is BusinessState.Loading -> FieldFocusResult.NoAction
            }
        } catch (e: Exception) {
            FieldFocusResult.Error("companyRegistrationNumber", e.message ?: if (AppLanguage.isArabic) "حدث خطأ غير متوقع" else "An unexpected error occurred")
        }
    }

    override suspend fun onStepOpened(stepIndex: Int) {
        val step = getSteps().getOrNull(stepIndex) ?: return

        // ✅ If this is the name change step, set current name value
        if (step.stepType == StepType.MARINE_UNIT_NAME_SELECTION) {
            println("📝 Name change step opened - setting current name value...")

            val selectedUnitsJson = accumulatedFormData["selectedMarineUnits"]
            if (selectedUnitsJson != null) {
                try {
                    val cleanJson = selectedUnitsJson.trim().removeSurrounding("[", "]")
                    val shipIds = cleanJson.split(",").map { it.trim().removeSurrounding("\"") }
                    val firstShipId = shipIds.firstOrNull()

                    if (firstShipId != null) {
                        val selectedShip = marineUnits.firstOrNull { it.id == firstShipId }
                        val shipName = selectedShip?.name

                        println("📍 Display current ship name: '$shipName'")

                        if (!shipName.isNullOrBlank()) {
                            accumulatedFormData["current_ship_name"] = shipName
                            println("✅ Setting current_ship_name = $shipName")
                        }
                    }
                } catch (e: Exception) {
                    println("⚠️ Failed to extract current ship name: ${e.message}")
                    e.printStackTrace()
                }
            }
        }

        // ✅ If this is the affected certificates step, load certificates from API
        if (step.stepType == StepType.AFFECTED_CERTIFICATES) {
            println("📜 ✅ Affected Certificates step opened - loading from API...")

            try {
                val shipInfoId = accumulatedFormData["shipInfoId"]?.toIntOrNull()
                    ?: accumulatedFormData["coreShipsInfoId"]?.toIntOrNull()
                    ?: throw ApiException(400, "Missing shipInfoId for affected certificates")

                println("📤 Calling affected-certificates API with shipInfoId=$shipInfoId, requestTypeId=$requestTypeId")

                val certificates = repository.getAffectedCertificates(
                    shipInfoId = shipInfoId,
                    requestTypeId = requestTypeId.toInt()
                )

                if (certificates.isSuccess) {
                    val affectedCerts = certificates.getOrNull() ?: emptyList()
                    println("✅ Loaded ${affectedCerts.size} affected certificates")

                    loadedCertificates.clear()
                    loadedCertificates.addAll(affectedCerts)

                    onStepsNeedRebuild?.invoke()
                } else {
                    val error = certificates.exceptionOrNull()?.message ?: if (AppLanguage.isArabic) "فشل تحميل الشهادات المتأثرة" else "Failed to load affected certificates"
                    println("❌ Failed to load certificates: $error")
                }
            } catch (e: Exception) {
                println("❌ Exception loading certificates: ${e.message}")
                e.printStackTrace()
            }
        }

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
                is StepProcessResult.Success -> {
                    println("✅ Payment data loaded successfully")
                }
                is StepProcessResult.Error -> {
                    println("❌ Payment error: ${paymentResult.message}")
                    accumulatedFormData["apiError"] = paymentResult.message
                }
                is StepProcessResult.NoAction -> {
                    println("ℹ️ No action needed for payment")
                }
            }
        }

        println("✅ Finished loading all lookups for step $stepIndex")

        // ✅ Rebuild steps after all lookups complete
        onStepsNeedRebuild?.invoke()
    }

    /**
     * Validate marine unit selection using TemporaryRegistrationRules
     * Called from MarineRegistrationViewModel when user clicks "Accept & Send" on review step
     */
    suspend fun validateMarineUnitSelection(unitId: String, userId: String): ValidationResult {
        return try {
            println("🔍 ChangeNameOfShipOrUnitStrategy: Validating unit $unitId using TemporaryRegistrationRules")

            // Find the selected unit
            val selectedUnit = marineUnits.firstOrNull { it.id == unitId }

            if (selectedUnit == null) {
                println("❌ Unit not found with id: $unitId")
                return ValidationResult.Error(if (AppLanguage.isArabic) "الوحدة البحرية المختارة غير موجودة" else "Selected marine unit not found")
            }

            println("✅ Found unit: ${selectedUnit.name}, id: ${selectedUnit.id}")

            // Use TemporaryRegistrationRules to validate
            val validationResult = temporaryRegistrationRules.validateUnit(selectedUnit, userId)
            val navigationAction = temporaryRegistrationRules.getNavigationAction(validationResult)

            println("✅ Validation result: ${validationResult::class.simpleName}")
            println("✅ Navigation action: ${navigationAction::class.simpleName}")

            ValidationResult.Success(
                validationResult = validationResult,
                navigationAction = navigationAction
            )
        } catch (e: Exception) {
            println("❌ Validation error: ${e.message}")
            e.printStackTrace()
            ValidationResult.Error(e.message ?: if (AppLanguage.isArabic) "فشل التحقق من حالة الفحص" else "Failed to verify inspection status")
        }
    }

    override fun extractCompletedStepsFromApiResponse(response: Any): Set<StepType> {
        // TODO: Parse the Change Name of Ship or Unit API response to determine completed steps
        val completedSteps = mutableSetOf<StepType>()

        println("⚠️ ChangeNameOfShipOrUnitStrategy: extractCompletedStepsFromApiResponse not yet implemented")
        println("   Response type: ${response::class.simpleName}")

        return completedSteps
    }

    /**
     * ✅ CRITICAL: Return accumulatedFormData so values set in onStepOpened
     * (like current_port_of_registry) are propagated to UI state
     */
    override fun getFormData(): Map<String, String> {
        return accumulatedFormData
    }
}
