package com.informatique.mtcit.business.transactions

import android.content.Context
import com.informatique.mtcit.R
import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.business.transactions.shared.RegistrationRequestManager
import com.informatique.mtcit.business.transactions.shared.ReviewManager
import com.informatique.mtcit.business.transactions.shared.StepProcessResult
import com.informatique.mtcit.business.transactions.shared.StepType
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.validation.rules.DimensionValidationRules
import com.informatique.mtcit.business.validation.rules.ValidationRule
import com.informatique.mtcit.common.ApiException
import com.informatique.mtcit.common.ErrorMessageExtractor
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.data.model.RequiredDocumentItem
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.data.repository.MarineUnitRepository
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.ui.components.DropdownSection
import com.informatique.mtcit.ui.components.PersonType
import com.informatique.mtcit.ui.components.SelectableItem
import com.informatique.mtcit.ui.repo.CompanyRepo
import com.informatique.mtcit.ui.viewmodels.StepData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import com.informatique.mtcit.util.UserHelper

/**
 * Strategy for Permanent Registration Certificate
 * DEMONSTRATION: Adds an extra "Previous Registration" step to show dynamic step addition
 */
class PermanentRegistrationStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val companyRepository: CompanyRepo,
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository,
    private val marineUnitRepository: MarineUnitRepository,
    private val marineUnitsApiService: com.informatique.mtcit.data.api.MarineUnitsApiService,
    private val registrationRequestManager: RegistrationRequestManager,
    private val reviewManager: ReviewManager,
    private val paymentManager: com.informatique.mtcit.business.transactions.shared.PaymentManager,
    private val shipSelectionManager: com.informatique.mtcit.business.transactions.shared.ShipSelectionManager,
    private val registrationApiService: com.informatique.mtcit.data.api.RegistrationApiService,
    private val inspectionFlowManager: com.informatique.mtcit.business.transactions.shared.InspectionFlowManager,  // ✅ NEW: Inspection flow manager
    @ApplicationContext private val appContext: Context  // ✅ Injected context
) : BaseTransactionStrategy() {

    // ✅ Transaction context with all API endpoints
    private val transactionContext: TransactionContext = TransactionType.PERMANENT_REGISTRATION_CERTIFICATE.context

    // Cache for loaded dropdown options
    private var portOptions: List<String> = emptyList()
    private var countryOptions: List<String> = emptyList()
    private var shipTypeOptions: List<String> = emptyList()
    private var shipCategoryOptions: List<String> = emptyList()
    private var marineActivityOptions: List<String> = emptyList()
    private var proofTypeOptions: List<String> = emptyList()
    private var engineTypeOptions: List<String> = emptyList()
    private var engineFuelTypeOptions: List<String> = emptyList()
    private var engineStatusOptions: List<String> = emptyList()
    private var typeOptions: List<PersonType> = emptyList()
    private var commercialOptions: List<SelectableItem> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()
    private var insuranceCompanyOptions: List<String> = emptyList() // ✅ Add insurance companies
    private var requiredDocuments: List<RequiredDocumentItem> = emptyList() // ✅ Store required documents

    // ✅ NEW: Store loaded inspection authorities
    private var loadedInspectionAuthorities: List<DropdownSection> = emptyList()
    // ✅ NEW: Store inspection-specific documents (separate from permanent registration documents)
    private var loadedInspectionDocuments: List<RequiredDocumentItem> = emptyList()

    // NEW: Store filtered ship types based on selected category
    private var filteredShipTypeOptions: List<String> = emptyList()
    private var isShipTypeFiltered: Boolean = false

    // ✅ الحل: اعمل cache للـ form data
    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf()

    // ✅ Store maritime identification data from ship selection
    private var selectedShipImoNumber: String? = null
    private var selectedShipMmsiNumber: String? = null
    private var selectedShipCallSign: String? = null
    private var needsMaritimeIdentification: Boolean = false

    private val requestTypeId = TransactionType.PERMANENT_REGISTRATION_CERTIFICATE.toRequestTypeId()
    private var requestId: Long? = null

    // ✅ Add init block to verify requestTypeId
    init {
        println("=" .repeat(60))
        println("🏗️ PermanentRegistrationStrategy INITIALIZED")
        println("   Transaction Type: PERMANENT_REGISTRATION_CERTIFICATE")
        println("   requestTypeId: '$requestTypeId'")
        println("   Expected value: '2'")
        println("   Actual value matches: ${requestTypeId == "2"}")
        println("=" .repeat(60))
    }

    // ✅ Allow ViewModel to set a callback when steps need to be rebuilt
    override var onStepsNeedRebuild: (() -> Unit)? = null

    /**
     * ✅ Override setHasAcceptanceFromApi to also store in formData
     * This ensures the payment success dialog can access it
     */
    override fun setHasAcceptanceFromApi(hasAcceptanceValue: Int?) {
        super.setHasAcceptanceFromApi(hasAcceptanceValue)
        // ✅ Store in formData so PaymentSuccessDialog can access it
        accumulatedFormData["hasAcceptance"] = (hasAcceptanceValue == 1).toString()
        println("🔧 PermanentRegistrationStrategy: Stored hasAcceptance in formData: ${accumulatedFormData["hasAcceptance"]}")
    }

    /**
     * Handle inspection dialog confirmation
     * Called when user clicks "Continue" on inspection required dialog
     * This will load inspection lookups and inject the inspection purpose step
     */
    suspend fun handleInspectionContinue() {
        println("🔍 PermanentRegistrationStrategy: User confirmed inspection requirement")
        println("   Loading inspection lookups...")

        try {
            // Get shipInfoId from accumulatedFormData
            val shipInfoIdStr = accumulatedFormData["coreShipsInfoId"]
                ?: accumulatedFormData["shipInfoId"]
                ?: run {
                    println("❌ No shipInfoId found in formData")
                    accumulatedFormData["apiError"] = "لم يتم العثور على معرف السفينة"
                    return
                }

            val shipInfoId = shipInfoIdStr.toIntOrNull() ?: run {
                println("❌ Invalid shipInfoId: $shipInfoIdStr")
                accumulatedFormData["apiError"] = "معرف السفينة غير صالح"
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
            accumulatedFormData["apiError"] = "فشل تحميل بيانات المعاينة: ${e.message}"
        }
    }

    /**
     * Handle inspection dialog cancel
     * Called when user clicks "Cancel" on inspection required dialog
     */
    fun handleInspectionCancel() {
        println("ℹ️ PermanentRegistrationStrategy: User cancelled inspection requirement")

        // Just clear the dialog flag and stay on review step
        accumulatedFormData.remove("showInspectionDialog")

        // Set flag to show that request is sent but pending inspection
        accumulatedFormData["requestPendingInspection"] = "true"
    }


    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        println("🔄 Loading ESSENTIAL lookups only (lazy loading enabled for step-specific lookups)...")

        // ✅ Load only ESSENTIAL lookups needed for initial steps
        // Step-specific lookups (ports, countries, ship types, etc.) will be loaded lazily via onStepOpened()

        // ✅ Get civilId from token
        val ownerCivilId = UserHelper.getOwnerCivilId(appContext)
        println("🔑 Owner CivilId from token: $ownerCivilId")

        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()

        // ✅ Handle null civilId - return empty list if no token
        val commercialRegistrations = if (ownerCivilId != null) {
            lookupRepository.getCommercialRegistrations(ownerCivilId).getOrNull() ?: emptyList()
        } else {
            emptyList()
        }

        // ✅ Fetch required documents from API
        println("📄 PermanentRegistration - Fetching required documents from API...")
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

        // Store in instance variables
        typeOptions = personTypes
        commercialOptions = commercialRegistrations
        requiredDocuments = requiredDocumentsList // ✅ Store documents

        return mapOf(
            "marineUnits" to emptyList<MarineUnit>(), // ✅ Empty initially
            "personType" to personTypes,
            "commercialRegistration" to commercialRegistrations
        )
    }

    /**
     * ✅ NEW: Load ships when user selects type and presses Next
     */
    override suspend fun loadShipsForSelectedType(formData: Map<String, String>): List<MarineUnit> {
        val personType = formData["selectionPersonType"]
        // ✅ FIXED: The actual field ID is "selectionData" not "commercialRegistration"
        val commercialReg = formData["selectionData"]

        println("=" .repeat(60))
        println("🔍 DEBUG: PermanentRegistrationStrategy.loadShipsForSelectedType")
        println("   Transaction Type: PERMANENT_REGISTRATION_CERTIFICATE")
        println("   requestTypeId value: '$requestTypeId' (type: ${requestTypeId.javaClass.simpleName})")
        println("   Expected: '2' (Permanent Registration)")
        println("=" .repeat(60))

        println("🚢 loadShipsForSelectedType called - personType=$personType, commercialReg=$commercialReg")

        // ✅ Get civilId from token instead of hardcoded value
        val ownerCivilIdFromToken = UserHelper.getOwnerCivilId(appContext)
        println("🔑 Owner CivilId from token: $ownerCivilIdFromToken")

        // ✅ UPDATED: For companies, use commercialReg (crNumber) from selectionData
        // For individuals, use ownerCivilId from token
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

        println("🔍 Calling loadShipsForOwner with:")
        println("   ownerCivilId: $ownerCivilId")
        println("   commercialRegNumber: $commercialRegNumber")
        println("   requestTypeId: $requestTypeId")
        println("=" .repeat(60))

        marineUnits = marineUnitRepository.loadShipsForOwner(
            ownerCivilId = ownerCivilId,
            commercialRegNumber = commercialRegNumber,
            // **********************************************************************************************************
            //Request Type Id
            requestTypeId = requestTypeId
        )

        println("✅ Loaded ${marineUnits.size} ships")
        marineUnits.forEach { unit ->
            println("   - ${unit.shipName} (ID: ${unit.id})")
        }

        return marineUnits
    }

    override suspend fun clearLoadedShips() {
        println("🧹 Clearing loaded ships cache")
        marineUnits = emptyList()
    }

    // ✅ Load lookups when a step is opened (lazy loading)
    override suspend fun onStepOpened(stepIndex: Int) {
        val step = getSteps().getOrNull(stepIndex) ?: return

        println("🔍 onStepOpened called for step $stepIndex")
        println("   Step title: ${step.titleRes}")

        // ✅ Load countries and insurance companies when insurance document step is opened
        if (step.stepType == StepType.INSURANCE_DOCUMENT) {
            println("🏥 Insurance Document step opening - loading countries and insurance companies...")

            var dataLoaded = false

            if (countryOptions.isEmpty()) {
                println("🌍 Loading countries...")
                countryOptions = lookupRepository.getCountries().getOrNull() ?: emptyList()
            }

            if (insuranceCompanyOptions.isEmpty()) {
                println("🏢 Loading insurance companies...")
                insuranceCompanyOptions = lookupRepository.getInsuranceCompanies().getOrNull() ?: emptyList()
                println("✅ Loaded ${insuranceCompanyOptions.size} insurance companies")
                dataLoaded = true
            }

            // ✅ Notify UI to refresh steps so dropdowns pick up new data
            if (dataLoaded) {
                println("🔄 Notifying UI to rebuild steps with new data...")
                onStepsNeedRebuild?.invoke()
            }
        }

        // ✅ NEW: If this is inspection purposes step, load inspection authorities
        if (step.stepType == StepType.INSPECTION_PURPOSES_AND_AUTHORITIES) {
            println("🔍 Inspection purposes step opened - loading inspection authorities...")

            val shipInfoId = accumulatedFormData["shipInfoId"]
            if (shipInfoId != null) {
                println("📥 shipInfoId available ($shipInfoId) - loading inspection lookups...")

                try {
                    // Load all inspection lookups using InspectionFlowManager
                    val lookups = inspectionFlowManager.loadInspectionLookups(shipInfoId.toInt())

                    // ✅ Store authorities in member variable
                    loadedInspectionAuthorities = lookups.authoritySections

                    // Store lookups in accumulatedFormData for the step to use
                    accumulatedFormData["inspectionPurposes"] = lookups.purposes.joinToString(",")
                    accumulatedFormData["inspectionPlaces"] = lookups.places.joinToString(",")

                    println("✅ Loaded inspection lookups:")
                    println("   - Purposes: ${lookups.purposes.size}")
                    println("   - Places: ${lookups.places.size}")
                    println("   - Authority sections: ${lookups.authoritySections.size}")

                    // Debug print authority sections
                    lookups.authoritySections.forEachIndexed { index, section ->
                        println("   Section $index: ${section.title} (${section.items.size} items)")
                        section.items.take(3).forEach { item ->
                            println("      - $item")
                        }
                    }

                    // Rebuild steps with new inspection data
                    println("🔄 Notifying UI to rebuild steps with inspection authorities...")
                    onStepsNeedRebuild?.invoke()

                } catch (e: Exception) {
                    println("❌ Failed to load inspection lookups: ${e.message}")
                    e.printStackTrace()
                    accumulatedFormData["apiError"] = "فشل تحميل بيانات المعاينة: ${e.message}"
                }
            } else {
                println("⚠️ shipInfoId not available - cannot load inspection authorities")
            }

            return // Done processing inspection step
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
        }
    }

    override fun updateAccumulatedData(data: Map<String, String>) {
        accumulatedFormData.putAll(data)
        println("📦 PermanentRegistration - Updated accumulated data: $accumulatedFormData")
    }

    /**
     * ✅ NEW: Return current form data including inspection dialog flags
     */
    override fun getFormData(): Map<String, String> {
        return accumulatedFormData.toMap()
    }

    override fun getContext(): TransactionContext {
        return transactionContext
    }

    /**
     * ✅ Get the RegistrationRequestManager for draft tracking
     */
    fun getRegistrationRequestManager(): RegistrationRequestManager {
        return registrationRequestManager
    }

    override fun getSteps(): List<StepData> {
        val steps = mutableListOf<StepData>()

        // Step 1: Person Type
        steps.add(SharedSteps.personTypeStep(typeOptions))

        // Step 2: Commercial Registration (فقط للشركات)
        val personType = accumulatedFormData["selectionPersonType"]
        if (personType == "شركة") {
            steps.add(SharedSteps.commercialRegistrationStep(commercialOptions))
        }

        // Step 3: Marine Unit Selection
        steps.add(
            SharedSteps.marineUnitSelectionStep(
                units = marineUnits,
                allowMultipleSelection = false,
                showAddNewButton = true,
                showOwnedUnitsWarning = true
            )
        )

        // ✅ Step 4 (Conditional): Maritime Identification - only if fields are missing
        if (needsMaritimeIdentification) {
            println("📋 Adding Maritime Identification Step")
            steps.add(
                SharedSteps.maritimeIdentificationStep(
                    imoNumber = selectedShipImoNumber,
                    mmsiNumber = selectedShipMmsiNumber,
                    callSign = selectedShipCallSign
                )
            )
        }

        // Step 5: Insurance Document (dynamic field based on country)
        val selectedCountry = accumulatedFormData["insuranceCountry"]

        // ✅ Check for both Arabic and English country names for Oman
        val isOman = selectedCountry == null ||
                     selectedCountry == "OM" ||
                     selectedCountry == "عمان" ||
                     selectedCountry.contains("عمان", ignoreCase = true)

        val insuranceStep = if (isOman) {
            // For Oman (default): Use dropdown with insurance company IDs
            SharedSteps.insuranceDocumentStep(
                countries = countryOptions,
                insuranceCompanies = insuranceCompanyOptions
            )
        } else {
            // For other countries: Create step with text field for company name
            createInsuranceDocumentStepWithTextField(countries = countryOptions)
        }
        steps.add(insuranceStep)

        // Step 6: Dynamic Documents (from API)
        println("🔍 DEBUG: requiredDocuments.size = ${requiredDocuments.size}")
        steps.add(
            SharedSteps.dynamicDocumentsStep(
                documents = requiredDocuments  // ✅ Pass documents from API
            )
        )

        // Step 7: Review
        steps.add(
            SharedSteps.reviewStep()
        )

        // ✅ NEW: Inspection Purpose Step (dynamically added when inspection is required)
        val showInspectionStep = accumulatedFormData["showInspectionStep"]?.toBoolean() ?: false
        if (showInspectionStep) {
            println("📋 Adding Inspection Purpose Step (dynamically injected)")

            // Parse lookups from formData
            val purposes = accumulatedFormData["inspectionPurposes"]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val places = accumulatedFormData["inspectionPlaces"]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

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

        // ✅ NEW: Payment Steps - Only show if we have requestId AND inspection is NOT required
        val hasRequestId = accumulatedFormData["requestId"] != null
        val inspectionRequired = accumulatedFormData["showInspectionDialog"]?.toBoolean() ?: false

        println("🔍 Payment step visibility check:")
        println("   hasRequestId: $hasRequestId")
        println("   inspectionRequired: $inspectionRequired")
        println("   showInspectionStep: $showInspectionStep")

        // ✅ Only show payment steps if we have requestId AND no inspection is pending
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

        return steps
    }

    /**
     * Create insurance document step with text field for company name (non-Oman countries)
     */
    private fun createInsuranceDocumentStepWithTextField(countries: List<String>): StepData {
        val fields = mutableListOf<FormField>()

        // Insurance Document Number (mandatory)
        fields.add(
            FormField.TextField(
                id = "insuranceDocumentNumber",
                labelRes = R.string.insurance_document_number_placeholder,
                placeholder = R.string.insurance_document_number_placeholder.toString(),
                mandatory = true
            )
        )

        // Country (mandatory)
        fields.add(
            FormField.DropDown(
                id = "insuranceCountry",
                labelRes = R.string.insurance_country_placeholder,
                options = countries,
                mandatory = true,
                placeholder = R.string.insurance_country_placeholder.toString()
            )
        )

        // Insurance Company Name as TextField (mandatory)
        fields.add(
            FormField.TextField(
                id = "insuranceCompany",
                labelRes = R.string.insurance_company_placeholder,
                placeholder = R.string.insurance_company_placeholder.toString(),
                mandatory = true
            )
        )

        // Insurance Expiry Date (mandatory)
        fields.add(
            FormField.DatePicker(
                id = "insuranceExpiryDate",
                labelRes = R.string.insurance_expiry_date,
                allowPastDates = false,
                mandatory = true
            )
        )

        // ✅ NO CR Number field - it's taken from selectionData automatically

        // Insurance Document Attachment (mandatory)
        fields.add(
            FormField.FileUpload(
                id = "insuranceDocumentFile",
                labelRes = R.string.insurance_document_attachment,
                allowedTypes = listOf("pdf", "jpg", "jpeg", "png"),
                maxSizeMB = 5,
                mandatory = true
            )
        )

        return StepData(
            stepType = StepType.INSURANCE_DOCUMENT,
            titleRes = R.string.insurance_document_title,
            descriptionRes = R.string.insurance_document_description,
            fields = fields
        )
    }

    override fun validateStep(
        step: Int,
        data: Map<String, Any>
    ): Pair<Boolean, Map<String, String>> {
        val stepData = getSteps().getOrNull(step) ?: return Pair(false, emptyMap())
        val formData = data.mapValues { it.value.toString() }

        // ✅ Get validation rules for this step
        val rules = getValidationRulesForStep(step, stepData)

        // ✅ Use validation with rules if available
        return if (rules.isNotEmpty()) {
            validationUseCase.validateStepWithAccumulatedData(
                stepData = stepData,
                currentStepData = formData,
                allAccumulatedData = accumulatedFormData,
                crossFieldRules = rules
            )
        } else {
            // Fallback to basic validation
            validationUseCase.validateStep(stepData, formData)
        }
    }

    /**
     * Get validation rules based on step content
     */
    private fun getValidationRulesForStep(stepIndex: Int, stepData: StepData): List<ValidationRule> {
        val fieldIds = stepData.fields.map { it.id }
        val rules = mutableListOf<ValidationRule>()

        // Dimension Rules
        // ✅ Check dimension fields don't exceed 99.99 meters
        if (fieldIds.any { it in listOf("overallLength", "overallWidth", "depth", "height") }) {
            rules.add(DimensionValidationRules.dimensionMaxValueValidation())
        }

        if (fieldIds.containsAll(listOf("overallLength", "overallWidth"))) {
            rules.add(DimensionValidationRules.lengthGreaterThanWidth())
        }

        if (fieldIds.containsAll(listOf("height", "grossTonnage"))) {
            rules.add(DimensionValidationRules.heightValidation())
        }

        if (fieldIds.containsAll(listOf("decksCount", "grossTonnage"))) {
            rules.add(DimensionValidationRules.deckCountValidation())
        }

        return rules
    }

    override suspend fun processStepData(step: Int, data: Map<String, String>): Int {
        println("🔄 processStepData called with: $data")

        // ✅ Update accumulated data
        accumulatedFormData.putAll(data)

        println("📦 accumulatedFormData after update: $accumulatedFormData")

        // ✅ Use RegistrationRequestManager to process step data
        val currentStepData = getSteps().getOrNull(step)
        if (currentStepData != null) {
            val stepType = currentStepData.stepType

            println("🔍 DEBUG - Step $step type: $stepType")
            println("🔍 DEBUG - Data keys: ${data.keys}")

            // ✅ NEW: Check if we just completed the Marine Unit Selection step
            if (currentStepData.titleRes == R.string.owned_ships) {
                val selectedUnitsJson = data["selectedMarineUnits"] ?: accumulatedFormData["selectedMarineUnits"]
                try {
                    // ✅ Use ShipSelectionManager
                    val result = shipSelectionManager.handleShipSelection(
                        shipId = selectedUnitsJson,
                        context = transactionContext
                    )

                    when (result) {
                        is com.informatique.mtcit.business.transactions.shared.ShipSelectionResult.Success -> {
                            println("✅ Ship selection successful!")
                            accumulatedFormData["requestId"] = result.requestId.toString()
                            requestId = result.requestId.toLong()

                            // ✅ NEW: Store maritime identification data
                            selectedShipImoNumber = result.imoNumber
                            selectedShipMmsiNumber = result.mmsiNumber
                            selectedShipCallSign = result.callSign
                            needsMaritimeIdentification = result.needsMaritimeIdentification

                            // Extract and persist selected shipInfoId (clean first element)
                            val selectedUnits = selectedUnitsJson?.let { sel ->
                                try {
                                    val cleanJson = sel.trim().removeSurrounding("[", "]")
                                    val shipIds = cleanJson.split(",").map { it.trim().removeSurrounding("\"") }
                                    shipIds.firstOrNull()
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            selectedUnits?.let { firstShipId ->
                                accumulatedFormData["shipInfoId"] = firstShipId
                                accumulatedFormData["coreShipsInfoId"] = firstShipId
                                // ensureRequestCreated expects selectedMarineUnit (singular)
                                accumulatedFormData["selectedMarineUnit"] = firstShipId
                            }

                            // ✅ Also update form data with maritime identification fields
                            accumulatedFormData["imoNumber"] = result.imoNumber ?: ""
                            accumulatedFormData["mmsiNumber"] = result.mmsiNumber ?: ""
                            accumulatedFormData["callSign"] = result.callSign ?: ""
                            accumulatedFormData["needsMaritimeIdentification"] =
                                result.needsMaritimeIdentification.toString()

                            println("📋 Maritime identification data stored:")
                            println("   needsMaritimeIdentification: ${result.needsMaritimeIdentification}")
                            println("   shipId: ${result.shipId}")
                            println("   imoNumber: ${result.imoNumber}")
                            println("   mmsiNumber: ${result.mmsiNumber}")
                            println("   callSign: ${result.callSign}")
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
                    val errorMsg =
                        com.informatique.mtcit.common.ErrorMessageExtractor.extract(e.message)
                    accumulatedFormData["apiError"] = errorMsg
                    throw ApiException(500, errorMsg)
                }
            }

            // ✅ Handle Insurance Document Step
            if (currentStepData.stepType == StepType.INSURANCE_DOCUMENT) {
                println("📄 ✅ Insurance Document step completed - calling validate-insurance-document API...")

                try {
                    // ✅ Get ship ID - handle both array format and single value
                    val shipIdString = accumulatedFormData["shipInfoId"]
                    val shipInfoId = when {
                        shipIdString == null -> throw ApiException(400, "Ship ID not found")
                        shipIdString.startsWith("[") -> {
                            // Array format: ["1674"] -> extract the number
                            shipIdString.trim('[', ']', '"').toIntOrNull()
                                ?: throw ApiException(400, "Invalid ship ID format")
                        }

                        else -> {
                            // Single value: "1674"
                            shipIdString.toIntOrNull()
                                ?: throw ApiException(400, "Invalid ship ID")
                        }
                    }

                    val insuranceNumber = data["insuranceDocumentNumber"]
                        ?: throw ApiException(400, "Insurance document number is required")

                    // ✅ Get country name from form and convert to country ID
                    val selectedCountryName = data["insuranceCountry"]
                        ?: throw ApiException(400, "Insurance country is required")

                    val countryId = lookupRepository.getCountryId(selectedCountryName)
                        ?: throw ApiException(
                            400,
                            "Could not find country ID for: $selectedCountryName"
                        )

                    println("🌍 Selected country: $selectedCountryName -> ID: $countryId")

                    val insuranceExpiryDate = data["insuranceExpiryDate"]
                        ?: throw ApiException(400, "Insurance expiry date is required")

                    // ✅ Get CR number from selectionData (for companies) or null (for individuals)
                    val selectedPersonType = accumulatedFormData["selectionPersonType"]
                    val crNumber = if (selectedPersonType == "شركة") {
                        // For companies: Get CR number from selectionData (commercial registration)
                        accumulatedFormData["selectionData"]
                            ?: throw ApiException(400, "Commercial registration number not found")
                    } else {
                        // For individuals: CR number is not required
                        null
                    }

                    // ✅ Handle company field based on country selection
                    // Check for both Arabic and English country names
                    val insuranceCompanyId: Int?
                    val insuranceCompanyName: String?

                    if (countryId == "OM" || countryId == "عمان" || countryId.contains(
                            "عمان",
                            ignoreCase = true
                        )
                    ) {
                        // For Oman: insuranceCompany dropdown returns company name, we need to get the ID
                        val selectedCompanyName = data["insuranceCompany"]
                            ?: throw ApiException(400, "Insurance company is required for Oman")

                        // ✅ Get company ID from name using lookupRepository
                        val companyIdString = lookupRepository.getInsuranceCompanyId(selectedCompanyName)
                            ?: throw ApiException(400, "Could not find insurance company ID for: $selectedCompanyName")

                        insuranceCompanyId = companyIdString.toIntOrNull()
                            ?: throw ApiException(400, "Invalid insurance company ID format")

                        insuranceCompanyName = null
                        println("🇴🇲 Oman selected - Company: $selectedCompanyName, ID: $insuranceCompanyId")
                    } else {
                        // For other countries: insuranceCompany contains the company name (text field)
                        insuranceCompanyId = null
                        insuranceCompanyName = data["insuranceCompany"]
                            ?: throw ApiException(400, "Insurance company name is required")
                        println("🌍 Other country selected - using insuranceCompanyName: $insuranceCompanyName")
                    }

                    // Get the file from form data
                    val fileUri = data["insuranceDocumentFile"]
                        ?: throw ApiException(400, "Insurance document file is required")

                    println("📋 Insurance Document Data:")
                    println("   Ship Info ID: $shipInfoId")
                    println("   Insurance Number: $insuranceNumber")
                    println("   Country ID: $countryId")
                    println("   Insurance Company ID: $insuranceCompanyId")
                    println("   Insurance Company Name: $insuranceCompanyName")
                    println("   Insurance Expiry Date: $insuranceExpiryDate")
                    println("   CR Number: $crNumber")
                    println("   Person Type: $selectedPersonType")
                    println("   File URI: $fileUri")

                    // ✅ Build the DTO
                    val insuranceDto =
                        com.informatique.mtcit.data.model.InsuranceDocumentRequestDto(
                            shipInfoId = shipInfoId,
                            insuranceNumber = insuranceNumber,
                            countryId = countryId,
                            insuranceCompanyId = insuranceCompanyId,
                            insuranceCompanyName = insuranceCompanyName,
                            insuranceExpiryDate = insuranceExpiryDate,
                            crNumber = crNumber
                        )

                    // ✅ Prepare file upload from URI
                    val fileBytes = try {
                        val uri = android.net.Uri.parse(fileUri)
                        val inputStream = appContext.contentResolver.openInputStream(uri)
                            ?: throw ApiException(400, "Failed to read file")
                        inputStream.readBytes()
                    } catch (e: Exception) {
                        println("❌ Error reading file: ${e.message}")
                        throw ApiException(400, "Failed to read insurance document file")
                    }

                    val fileName = fileUri.substringAfterLast("/")
                    val mimeType =
                        appContext.contentResolver.getType(android.net.Uri.parse(fileUri))
                            ?: "application/octet-stream"

                    val fileUpload = com.informatique.mtcit.data.model.DocumentFileUpload(
                        fileName = fileName,
                        fileUri = fileUri,
                        fileBytes = fileBytes,
                        mimeType = mimeType,
                        documentId = 0 // Not used for insurance document
                    )

                    println("📤 Calling validateInsuranceDocument API...")

                    // ✅ Call the API
                    val apiResult = registrationApiService.validateInsuranceDocument(
                        insuranceDto = insuranceDto,
                        file = fileUpload
                    )

                    apiResult.fold(
                        onSuccess = { response ->
                            println("✅ Insurance document validated successfully!")
                            println("   Message: ${response.message}")
                            println("   Request ID: ${response.data?.id}")
                            println("   Status ID: ${response.data?.status?.id}")

                            // ✅ Store response data and UPDATE requestId from insurance step
                            accumulatedFormData["insuranceValidationMessage"] = response.message
                            response.data?.id?.let {
                                // ✅ Update requestId to use the ID from insurance step response
                                accumulatedFormData["requestId"] = it.toString()
                                requestId = it.toLong()
                                println("✅ RequestId updated from insurance step: $it")
                            }
                        },
                        onFailure = { error ->
                            println("⚠️ Failed to check inspection preview: ${error.message}")

                            // Build friendly message and store for UI/debugging
                            val msg = when (error) {
                                is ApiException -> error.message ?: "فشل في التحقق من المعاينة"
                                else -> ErrorMessageExtractor.extract(error.message)
                            }

                            accumulatedFormData["apiError"] = "حدث خطأ أثناء التحقق من المعاينة: $msg"
                            // store lastApiError if available (strategy holds it)
                            try {
                                val field = this::class.java.getDeclaredField("lastApiError")
                                field.isAccessible = true
                                field.set(this, msg)
                            } catch (_: Exception) {
                                // best-effort: if lastApiError not present, ignore
                            }

                            // Re-throw so central ViewModel (BaseTransactionViewModel) can show ErrorBanner
                            if (error is ApiException) throw error else throw ApiException(500, msg)
                        }
                    )
                } catch (e: ApiException) {
                    println("❌ ApiException in insurance document: ${e.message}")
                    accumulatedFormData["apiError"] = e.message ?: "Unknown error"
                    throw e // Re-throw to show error banner
                } catch (e: Exception) {
                    println("❌ Exception in insurance document: ${e.message}")
                    val errorMsg =
                        com.informatique.mtcit.common.ErrorMessageExtractor.extract(e.message)
                    accumulatedFormData["apiError"] = errorMsg
                    throw ApiException(500, errorMsg)
                }
            }


                // ✅ Call RegistrationRequestManager to process registration-related steps
                val result = registrationRequestManager.processStepIfNeeded(
                    stepType = stepType,
                    formData = accumulatedFormData,
                    requestTypeId = requestTypeId, // 2 = Permanent Registration
                    context = appContext
                )

                when (result) {
                    is StepProcessResult.Success -> {
                        println("✅ ${result.message}")
                    }

                    is StepProcessResult.Error -> {
                        println("❌ Error: ${result.message}")
                        accumulatedFormData["apiError"] = result.message
                        return -1 // Block navigation on error
                    }

                    is StepProcessResult.NoAction -> {
                        println("ℹ️ No registration action needed for this step")

                        // ✅ HANDLE REVIEW STEP - Use ReviewManager
                        if (stepType == StepType.REVIEW) {
                            println("📋 Handling Review Step using ReviewManager")

                            val requestIdInt = accumulatedFormData["requestId"]?.toIntOrNull()
                            if (requestIdInt == null) {
                                println("❌ No requestId available for review step")
                                accumulatedFormData["apiError"] = "لم يتم العثور على رقم الطلب"
                                return -1
                            }

                            try {
                                // ✅ STEP 1: ALWAYS Send request FIRST
                                println("🚀 STEP 1: Calling ReviewManager.processReviewStep (ALWAYS submit first)...")

                                // ✅ Get endpoint and context from transactionContext
                                val transactionContext =
                                    TransactionType.PERMANENT_REGISTRATION_CERTIFICATE.context
                                val endpoint = transactionContext.sendRequestEndpoint.replace(
                                    "{requestId}",
                                    requestIdInt.toString()
                                )
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

                                        // ✅ NEW: Check hasAcceptance flag from metadata
                                        if (reviewResult.hasAcceptance) {
                                            println("🛑 hasAcceptance=1: Transaction requires acceptance/approval")
                                            println("   Stopping transaction - user must continue from profile later")

                                            // Extract request number
                                            val requestNumber = reviewResult.additionalData?.get("requestNumber")?.toString()
                                                ?: reviewResult.additionalData?.get("requestSerial")?.toString()
                                                ?: accumulatedFormData["requestSerial"]
                                                ?: accumulatedFormData["requestId"]
                                                ?: "N/A"

                                            // Store success message for dialog
                                            accumulatedFormData["successMessage"] = reviewResult.message
                                            accumulatedFormData["requestNumber"] = requestNumber

                                            // Return -2 to indicate: success but stop transaction (show dialog)
                                            return -2
                                        }

                                        // ✅ Check if inspection is required (from API response)
                                        if (inspectionFlowManager.isInspectionRequired(reviewResult.needInspection)) {
                                            println("🔍 Inspection is required - preparing dialog")

                                            // Get requestId for parent tracking
                                            val requestId = accumulatedFormData["requestId"]?.toIntOrNull()

                                            // Prepare inspection dialog with parent transaction info
                                            // Request Type: 2 = Permanent Registration
                                            inspectionFlowManager.prepareInspectionDialog(
                                                message = reviewResult.message,
                                                formData = accumulatedFormData,
                                                parentRequestId = requestId,
                                                parentRequestType = 2  // Permanent Registration
                                            )

                                            println("⚠️ Inspection required - showing dialog and blocking proceed")
                                            return step // Stay on current step to show dialog
                                        }

                                        // ✅ Extract request number
                                        val requestNumber = reviewResult.additionalData?.get("requestNumber")?.toString()
                                            ?: reviewResult.additionalData?.get("requestSerial")?.toString()
                                            ?: accumulatedFormData["requestSerial"]
                                            ?: accumulatedFormData["requestId"] // Use requestId if available
                                            ?: "N/A"

                                        // ✅ NEW: Check if this is a NEW request (not resumed)
                                        val isNewRequest = accumulatedFormData["isResumedTransaction"]?.toBoolean() != true

                                        println("🔍 isNewRequest check:")
                                        println("   - isResumedTransaction flag: ${accumulatedFormData["isResumedTransaction"]}")
                                        println("   - isNewRequest result: $isNewRequest")

                                        // ✅ STEP 2: Check inspection (AFTER request is sent successfully)
                                        println("🔍 STEP 2: Checking inspection preview (request already sent)...")

                                        try {
                                            // Get shipInfoId from formData
                                            val shipIdString = accumulatedFormData["shipInfoId"]
                                            val shipInfoId = when {
                                                shipIdString == null -> {
                                                    println("❌ Ship ID not found in formData - skipping inspection check")
                                                    null
                                                }
                                                shipIdString.startsWith("[") -> {
                                                    // Array format: ["1674"] -> extract the number
                                                    shipIdString.trim('[', ']', '"').toIntOrNull()
                                                }
                                                else -> {
                                                    // Single value: "1674"
                                                    shipIdString.toIntOrNull()
                                                }
                                            }

                                            if (shipInfoId != null && requestId != null) {
                                                println("   Calling checkInspectionPreview with requestId: $requestId")
                                                val inspectionResult = marineUnitRepository.checkInspectionPreview(
                                                    requestId!!.toInt(), transactionContext.inspectionPreviewBaseContext)

                                                // ✅ Handle inspection status using InspectionFlowManager
                                                inspectionResult.fold(
                                                    onSuccess = { inspectionStatus ->
                                                        println("✅ Inspection preview check successful")
                                                        println("   Inspection status: $inspectionStatus (0=needs inspection, 1=has inspection)")

                                                        if (inspectionStatus == 0) {
                                                            // ⚠️ Ship requires inspection - show dialog (but request is already sent!)
                                                            println("⚠️ Ship requires inspection - preparing dialog")
                                                            println("📋 Permanent registration request was ALREADY submitted successfully")
                                                            println("📋 The backend will update status automatically after inspection is done")

                                                            // ✅ Use manager to prepare dialog (sets all flags) with parent transaction info
                                                            // Request Type: 2 = Permanent Registration
                                                            inspectionFlowManager.prepareInspectionDialog(
                                                                message = "تم إرسال طلب التسجيل الدائم بنجاح (رقم الطلب: $requestNumber).\n\nالسفينة تحتاج إلى معاينة لإكمال الإجراءات. يرجى الاستمرار لتقديم طلب معاينة.",
                                                                formData = accumulatedFormData,
                                                                parentRequestId = requestId?.toInt(),  // Convert Long to Int
                                                                parentRequestType = 2  // Permanent Registration
                                                            )

                                                            println("⚠️ Inspection required - dialog will be shown")
                                                            return step // Stay on current step to show dialog

                                                        } else {
                                                            // ✅ Inspection done - proceed with normal flow
                                                            println("✅ Ship has inspection completed - proceeding with normal success flow")
                                                        }
                                                    },
                                                    onFailure = { error ->
                                                        println("⚠️ Failed to check inspection preview: ${error.message}")

                                                        // Build friendly message and store for UI/debugging
                                                        val msg = when (error) {
                                                            is ApiException -> error.message ?: "فشل في التحقق من المعاينة"
                                                            else -> ErrorMessageExtractor.extract(error.message)
                                                        }

                                                        accumulatedFormData["apiError"] = "حدث خطأ أثناء التحقق من المعاينة: $msg"
                                                        // store lastApiError if available (strategy holds it)
                                                        try {
                                                            val field = this::class.java.getDeclaredField("lastApiError")
                                                            field.isAccessible = true
                                                            field.set(this, msg)
                                                        } catch (_: Exception) {
                                                            // best-effort: if lastApiError not present, ignore
                                                        }

                                                        // Re-throw so central ViewModel (BaseTransactionViewModel) can show ErrorBanner
                                                        if (error is ApiException) throw error else throw ApiException(500, msg)
                                                    }
                                                )
                                            } else {
                                                println("⚠️ Missing shipInfoId or requestId - skipping inspection check")
                                            }
                                        } catch (e: Exception) {
                                            println("⚠️ Exception checking inspection: ${e.message}")
                                            println("✅ But request was already sent successfully - continuing with normal flow")
                                            // Continue with normal flow since main request was successful
                                        }

                                        // ✅ Continue with normal success flow (NEW or resumed request)

                                        // ✅ NEW: Check hasAcceptance flag from transaction context
                                        val hasAcceptance = transactionContext.hasAcceptance

                                        println("🔍 Post-submission flow decision:")
                                        println("   - isNewRequest: $isNewRequest")
                                        println("   - hasAcceptance: $hasAcceptance")

                                        // ✅ Use hasAcceptance from strategy property (set from TransactionDetail API)
                                        val strategyHasAcceptance = this.hasAcceptance

                                        // ✅ Only stop if BOTH isNewRequest AND hasAcceptance are true
                                        if (isNewRequest && strategyHasAcceptance) {
                                            println("🎉 NEW request submitted with hasAcceptance=true - showing success dialog and stopping")
                                            println("   User must continue from profile screen")

                                            // Set success flags for ViewModel to show dialog
                                            accumulatedFormData["requestSubmitted"] = "true"
                                            accumulatedFormData["requestNumber"] = requestNumber
                                            accumulatedFormData["successMessage"] = reviewResult.message

                                            // Return -2 to indicate: success but show dialog and stop
                                            return -2
                                        } else if (isNewRequest && !strategyHasAcceptance) {
                                            println("✅ NEW request submitted with hasAcceptance=false - continuing to next steps")
                                            println("   Transaction will continue to payment/next steps")
                                            // Continue normally - don't return, let the flow proceed
                                        } else {
                                            println("✅ Resumed request - showing success dialog")
                                            accumulatedFormData["showSuccessAlert"] = "true"
                                            accumulatedFormData["successAlertMessage"] = reviewResult.message
                                            return step // Stay on current step to show alert
                                        }
                                    }

                                    is com.informatique.mtcit.business.transactions.shared.ReviewResult.Error -> {
                                        println("❌ Review step failed: ${reviewResult.message}")
                                        accumulatedFormData["apiError"] = reviewResult.message
                                        return -1 // Block navigation
                                    }
                                }
                            } catch (e: Exception) {
                                println("❌ Exception in review step: ${e.message}")
                                e.printStackTrace()
                                accumulatedFormData["apiError"] =
                                    "حدث خطأ أثناء إرسال الطلب: ${e.message}"
                                return -1
                            }
                        }

                        // ✅ NEW: Handle Inspection Purpose Step
                        if (inspectionFlowManager.isInspectionPurposeStep(stepType)) {
                            println("🔍 Processing Inspection Purpose Step...")

                            try {
                                val inspectionResult = inspectionFlowManager.handleInspectionPurposeStepCompletion(
                                    formData = accumulatedFormData,
                                    context = appContext
                                )

                                when (inspectionResult) {
                                    is StepProcessResult.Success -> {
                                        println("✅ Inspection request submitted successfully!")
                                        println("   Message: ${inspectionResult.message}")

                                        // ✅ IMPORTANT: Exit the transaction completely
                                        // When inspection is submitted from within another transaction,
                                        // we should show success dialog and exit (like standalone inspection transaction)

                                        // Set success flags for ViewModel to show dialog
                                        accumulatedFormData["inspectionRequestSubmitted"] = "true"
                                        accumulatedFormData["showInspectionSuccessDialog"] = "true"
                                        accumulatedFormData["inspectionSuccessMessage"] = inspectionResult.message

                                        println("🎉 Inspection submitted - exiting transaction (returning -3)")

                                        // Return -3 to indicate: inspection success, show dialog and exit transaction
                                        return -3
                                    }
                                    is StepProcessResult.Error -> {
                                        println("❌ Inspection request submission failed: ${inspectionResult.message}")
                                        accumulatedFormData["apiError"] = inspectionResult.message
                                        return -1 // Block navigation
                                    }
                                    is StepProcessResult.NoAction -> {
                                        println("ℹ️ No action taken for inspection step")
                                        // This shouldn't happen for inspection purpose step, but handle it
                                    }
                                }
                            } catch (e: Exception) {
                                println("❌ Exception processing inspection step: ${e.message}")
                                e.printStackTrace()
                                accumulatedFormData["apiError"] = "حدث خطأ أثناء إرسال طلب المعاينة: ${e.message}"
                                return -1
                            }
                        }

                        val paymentResult = paymentManager.processStepIfNeeded(
                            stepType = stepType,
                            formData = accumulatedFormData,
                            requestTypeId = requestTypeId.toInt(),
                            context = transactionContext // ✅ Pass TransactionContext
                        )

                        when (paymentResult) {
                            is StepProcessResult.Success -> {
                                println("✅ Payment step processed: ${paymentResult.message}")

                                // Check if payment was successful and trigger step rebuild
                                if (stepType == StepType.PAYMENT_CONFIRMATION) {
                                    val paymentSuccessful = accumulatedFormData["paymentSuccessful"]?.toBoolean() ?: false
                                    if (paymentSuccessful) {
                                        println("✅ Payment successful - triggering step rebuild")
                                        onStepsNeedRebuild?.invoke()
                                    }
                                }

                                // Check if we loaded payment details and trigger step rebuild
                                if (stepType == StepType.PAYMENT) {
                                    println("✅ Payment details loaded - triggering step rebuild")
                                    onStepsNeedRebuild?.invoke()
                                }
                            }
                            is StepProcessResult.Error -> {
                                println("❌ Payment error: ${paymentResult.message}")
                            }
                            is StepProcessResult.NoAction -> {
                                println("ℹ️ No payment action needed for this step")
                            }
                        }
                    }
                }
            }


        return step
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        return repository.submitRegistration(data)
    }

    override fun handleFieldChange(fieldId: String, value: String, formData: Map<String, String>): Map<String, String> {
        val mutableFormData = formData.toMutableMap()

        // ❌ REMOVED: Duplicate inspection step injection handling
        // This is now handled ONLY in MarineRegistrationScreen.kt LaunchedEffect
        // to avoid double-triggering and maintain proper architecture

        // ✅ Handle insurance country change - switch between dropdown and text field for company
        if (fieldId == "insuranceCountry" && value.isNotBlank()) {
            println("🏢 Insurance country changed to: $value")

            // Clear the insurance company field when country changes
            mutableFormData.remove("insuranceCompany")

            // Trigger step refresh to update the field type
            mutableFormData["_triggerRefresh"] = "true"

            println("✅ Insurance company field cleared and step refresh triggered")
            return mutableFormData
        }

        // NEW: Handle ship category change - fetch filtered ship types
        if (fieldId == "unitClassification" && value.isNotBlank()) {
            println("🚢 Ship category changed to: $value")

            // Get category ID from category name
            val categoryId = lookupRepository.getShipCategoryId(value)

            if (categoryId != null) {
                println("🔍 Found category ID: $categoryId")

                // Fetch filtered ship types
                kotlinx.coroutines.runBlocking {
                    val filteredTypes = lookupRepository.getShipTypesByCategory(categoryId).getOrNull()
                    if (filteredTypes != null && filteredTypes.isNotEmpty()) {
                        println("✅ Loaded ${filteredTypes.size} ship types for category $categoryId")
                        filteredShipTypeOptions = filteredTypes
                        isShipTypeFiltered = true

                        // Clear the unitType field since the options changed
                        mutableFormData.remove("unitType")

                        // Add a flag to trigger step refresh
                        mutableFormData["_triggerRefresh"] = "true"
                    } else {
                        println("⚠️ No ship types found for category $categoryId")
                        filteredShipTypeOptions = emptyList()
                        isShipTypeFiltered = true
                        mutableFormData.remove("unitType")
                        mutableFormData["_triggerRefresh"] = "true"
                    }
                }
            } else {
                println("❌ Could not find category ID for: $value")
            }

            return mutableFormData
        }

        if (fieldId == "owner_type") {
            when (value) {
                "فرد" -> {
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
            return FieldFocusResult.Error("companyRegistrationNumber", "رقم السجل التجاري مطلوب")
        }


        if (registrationNumber.length < 3) {
            return FieldFocusResult.Error("companyRegistrationNumber", "رقم السجل التجاري يجب أن يكون أكثر من 3 أرقام")
        }

        return try {
            val result = companyRepository.fetchCompanyLookup(registrationNumber)
                .flowOn(Dispatchers.IO)
                .catch { throw Exception("حدث خطأ أثناء البحث عن الشركة: ${it.message}") }
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
                        FieldFocusResult.Error("companyRegistrationNumber", "لم يتم العثور على الشركة")
                    }
                }
                is BusinessState.Error -> FieldFocusResult.Error("companyRegistrationNumber", result.message)
                is BusinessState.Loading -> FieldFocusResult.NoAction
            }
        } catch (e: Exception) {
            FieldFocusResult.Error("companyRegistrationNumber", e.message ?: "حدث خطأ غير متوقع")
        }
    }

    // ================================================================================
    // 🎯 DRAFT TRACKING: Extract completed steps from API response
    // ================================================================================
    override fun extractCompletedStepsFromApiResponse(response: Any): Set<StepType> {
        // TODO: Parse the permanent registration API response and determine which steps are completed
        // Example structure based on API response:
        // {
        //   "data": {
        //     "id": 2072,
        //     "shipInfo": { ... },  // → MARINE_UNIT_DATA, SHIP_DIMENSIONS, SHIP_WEIGHTS
        //     "engines": [...],     // → ENGINE_INFO
        //     "owners": [...],      // → OWNER_INFO
        //     "documents": [...],   // → DOCUMENTS
        //     "insurance": {...},   // → INSURANCE_INFO
        //     "status": {...}
        //   }
        // }

        val completedSteps = mutableSetOf<StepType>()

        // For now, return empty set - this will be populated when we have the actual response structure
        println("⚠️ PermanentRegistrationStrategy: extractCompletedStepsFromApiResponse not yet implemented")
        println("   Response type: ${response::class.simpleName}")

        return completedSteps
    }
}
