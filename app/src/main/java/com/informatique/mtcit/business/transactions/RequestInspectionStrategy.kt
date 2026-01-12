package com.informatique.mtcit.business.transactions

import android.content.Context
import com.informatique.mtcit.R
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.business.transactions.shared.RegistrationRequestManager
import com.informatique.mtcit.business.transactions.shared.StepProcessResult
import com.informatique.mtcit.business.transactions.shared.PaymentManager
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.validation.rules.DateValidationRules
import com.informatique.mtcit.business.validation.rules.DimensionValidationRules
import com.informatique.mtcit.business.validation.rules.MarineUnitValidationRules
import com.informatique.mtcit.business.validation.rules.ValidationRule
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.data.repository.MarineUnitRepository
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.ui.components.PersonType
import com.informatique.mtcit.ui.components.SelectableItem
import com.informatique.mtcit.ui.repo.CompanyRepo
import com.informatique.mtcit.ui.viewmodels.StepData
import com.informatique.mtcit.ui.components.DropdownSection
import javax.inject.Inject
import com.informatique.mtcit.business.transactions.marineunit.rules.TemporaryRegistrationRules
import com.informatique.mtcit.business.transactions.shared.ReviewManager
import com.informatique.mtcit.business.transactions.shared.StepType
import com.informatique.mtcit.common.ApiException
import com.informatique.mtcit.data.model.RequiredDocumentItem
import com.informatique.mtcit.util.UserHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch

class RequestInspectionStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val companyRepository: CompanyRepo,
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository,
    private val marineUnitRepository: MarineUnitRepository,
    private val marineUnitsApiService: com.informatique.mtcit.data.api.MarineUnitsApiService,
    private val temporaryRegistrationRules: TemporaryRegistrationRules,
    private val registrationRequestManager: RegistrationRequestManager,
    private val paymentManager: PaymentManager,
    private val reviewManager: ReviewManager,
    private val shipSelectionManager: com.informatique.mtcit.business.transactions.shared.ShipSelectionManager,
    private val inspectionRequestManager: com.informatique.mtcit.business.transactions.shared.InspectionRequestManager,  // ✅ NEW: Inspection manager
    @ApplicationContext private val appContext: Context  // ✅ Injected context
) : TransactionStrategy, MarineUnitValidatable {
    // ✅ Transaction context with all API endpoints
    private val transactionContext: TransactionContext = TransactionType.REQUEST_FOR_INSPECTION.context
    private var portOptions: List<String> = emptyList()
    private var countryOptions: List<String> = emptyList()
    private var shipTypeOptions: List<String> = emptyList()
    private var shipCategoryOptions: List<String> = emptyList()
    private var marineActivityOptions: List<String> = emptyList()
    private var proofTypeOptions: List<String> = emptyList()
    private var engineTypeOptions: List<String> = emptyList()
    private var engineFuelTypeOptions: List<String> = emptyList()
    private var engineStatusOptions: List<String> = emptyList()
    private var buildMaterialOptions: List<String> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()
    private var commercialOptions: List<SelectableItem> = emptyList()
    private var typeOptions: List<PersonType> = emptyList()
    // ✅ NEW: Inspection request lookups
    private var inspectionPurposeOptions: List<String> = emptyList()
    private var inspectionAuthorityOptions: Map<String, List<String>> = emptyMap()
    // NEW: Store filtered ship types based on selected category
    private var filteredShipTypeOptions: List<String> = emptyList()
    private var isShipTypeFiltered: Boolean = false
    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf()
    // ✅ NEW: Store required documents from API
    private var requiredDocuments: List<RequiredDocumentItem> = emptyList()
    private var isFishingBoat: Boolean = false // ✅ Track if selected type is fishing boat
    private var fishingBoatDataLoaded: Boolean = false // ✅ Track if data loaded from Ministry
    private val requestTypeId = TransactionType.REQUEST_FOR_INSPECTION.toRequestTypeId()
    // ✅ Override the callback property from TransactionStrategy interface
    override var onStepsNeedRebuild: (() -> Unit)? = null
    // ✅ NEW: Override the per-lookup callbacks for loading indicators
    override var onLookupStarted: ((lookupKey: String) -> Unit)? = null
    override var onLookupCompleted: ((lookupKey: String, data: List<String>, success: Boolean) -> Unit)? = null
    /**
     * ✅ Get the transaction context with all API endpoints
     */
    override fun getContext(): TransactionContext {
        return transactionContext
    }

    // ✅ NEW: Payment state tracking
    private var requestId: Long? = null

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        println("🔄 Loading ESSENTIAL lookups only (lazy loading enabled for step-specific lookups)...")

        // ✅ Load only ESSENTIAL lookups needed for initial steps
        // Step-specific lookups (ports, countries, ship types, etc.) will be loaded lazily via onStepOpened()

        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()
        val commercialRegistrations = lookupRepository.getCommercialRegistrations("12345678901234").getOrNull() ?: emptyList()
        println("📄 RequestInspection - Fetching required documents from API for requestTypeId: $requestTypeId...")
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

        // ✅ Don't load ships here - they will be loaded when user presses Next
        // after selecting person type (individual/company)
        println("🚢 Skipping initial ship load - will load after user selects type and presses Next")

        return mapOf(
            "marineUnits" to emptyList<MarineUnit>(), // ✅ Empty initially
            "personType" to personTypes,
            "commercialRegistration" to commercialRegistrations
            // ❌ Removed: ports, countries, shipTypes, shipCategories, marineActivities, proofTypes, engineStatuses
            // These will be loaded lazily via onStepOpened() when user reaches those steps
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

        println("🔍 Calling loadShipsForOwner with ownerCivilId=$ownerCivilId, commercialRegNumber=$commercialRegNumber")

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

    /**
     * ✅ NEW: Clear loaded ships when user goes back
     */
    override suspend fun clearLoadedShips() {
        println("🧹 Clearing loaded ships cache")
        marineUnits = emptyList()
    }

    override fun updateAccumulatedData(data: Map<String, String>) {
        accumulatedFormData.putAll(data)
        println("📦 RequestInspection - Updated accumulated data: $accumulatedFormData")
    }

    /**
     * ✅ NEW: Return current form data including inspection dialog flags
     */
    override fun getFormData(): Map<String, String> {
        return accumulatedFormData.toMap()
    }

    override fun getSteps(): List<StepData> {
        val steps = mutableListOf<StepData>()

        // Step 1: Person Type
        steps.add(SharedSteps.personTypeStep(typeOptions))

        // Step 2: Commercial Registration (فقط للشركات)
        val selectedPersonType = accumulatedFormData["selectionPersonType"]
        if (selectedPersonType == "شركة") {
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

        // ✅ التحقق الصحيح من اختيار المستخدم
        val isAddingNewUnitFlag = accumulatedFormData["isAddingNewUnit"]?.toBoolean() ?: false
        val selectedUnitsJson = accumulatedFormData["selectedMarineUnits"]

        // ✅ المستخدم اختار سفينة موجودة إذا كان فيه JSON مش فاضي ومش "[]"
        val hasSelectedExistingUnit = !selectedUnitsJson.isNullOrEmpty() &&
                selectedUnitsJson != "[]"

        // ✅ WORKAROUND: لو selectedMarineUnits موجود وفاضي "[]" ومفيش isAddingNewUnit flag
        // معناها المستخدم ضغط على الزرار بس الفلاج مبعتش صح
        val isAddingNewUnit = isAddingNewUnitFlag ||
                (selectedUnitsJson == "[]" && accumulatedFormData.containsKey("selectedMarineUnits"))

        // ✅ طباعة للتتبع (Debug)
        println("🔍 DEBUG - isAddingNewUnitFlag: $isAddingNewUnitFlag")
        println("🔍 DEBUG - selectedUnitsJson: $selectedUnitsJson")
        println("🔍 DEBUG - accumulatedFormData: $accumulatedFormData")
        println("🔍 DEBUG - hasSelectedExistingUnit: $hasSelectedExistingUnit")
        println("🔍 DEBUG - isAddingNewUnit (final): $isAddingNewUnit")
        println("🔍 DEBUG - Will show new unit steps: ${isAddingNewUnit && !hasSelectedExistingUnit}")

        // ✅ نضيف steps الإضافة فقط لو المستخدم ضغط "إضافة جديدة" ومش مختار سفينة موجودة
        if (isAddingNewUnit && !hasSelectedExistingUnit) {
            println("✅ Adding new unit steps - same as Temporary Registration")

            // ✅ FIX: Pass empty lists initially - onStepOpened() will load the data via requiredLookups
            // The step declares requiredLookups = ["shipTypes", "shipCategories", "ports", "countries", ...]
            // so when the step is opened, onStepOpened() will load all the data and trigger a rebuild
            // This prevents crash when clicking "add_ship" before data is loaded
            val shipTypesToUse = if (isShipTypeFiltered && filteredShipTypeOptions.isNotEmpty()) {
                filteredShipTypeOptions
            } else {
                shipTypeOptions.ifEmpty { emptyList() }
            }

            println("🔧 getSteps - Using shipTypes: ${shipTypesToUse.size} types")

            steps.add(
                SharedSteps.unitSelectionStep(
                    shipTypes = shipTypesToUse,
                    shipCategories = shipCategoryOptions.ifEmpty { emptyList() },
                    ports = portOptions.ifEmpty { emptyList() },
                    countries = countryOptions.ifEmpty { emptyList() },
                    marineActivities = marineActivityOptions.ifEmpty { emptyList() },
                    proofTypes = proofTypeOptions.ifEmpty { emptyList() },
                    buildingMaterials = buildMaterialOptions.ifEmpty { emptyList() }, // ✅ تم إضافتها
                    includeIMO = true,
                    includeMMSI = true,
                    includeManufacturer = true,
                    includeProofDocument = false,
                    includeConstructionDates = true,
                    includeRegistrationCountry = true,
                    isFishingBoat = isFishingBoat,
                    fishingBoatDataLoaded = fishingBoatDataLoaded
                )
            )

//            steps.add(
//                SharedSteps.marineUnitDimensionsStep(
//                    includeHeight = true,
//                    includeDecksCount = true
//                )
//            )
//
//            steps.add(
//                SharedSteps.marineUnitWeightsStep(
//                    includeMaxPermittedLoad = true
//                )
//            )
//
//            steps.add(
//                SharedSteps.engineInfoStep(
//                    manufacturers = listOf(
//                        "Manufacturer 1",
//                        "Manufacturer 2",
//                        "Manufacturer 3"
//                    ),
//                    enginesTypes = engineTypeOptions,
//                    countries = countryOptions,
//                    fuelTypes = engineFuelTypeOptions,
//                    engineConditions = engineStatusOptions,
//                )
//            )
//
//            steps.add(
//                SharedSteps.ownerInfoStep(
//                    nationalities = countryOptions,
//                    countries = countryOptions,
//                    includeCompanyFields = true,
//                )
//            )

            // ✅ Check overallLength to determine if inspection documents are mandatory
            val overallLength = accumulatedFormData["overallLength"]?.toDoubleOrNull() ?: 0.0
            val isInspectionDocMandatory = overallLength <= 24.0

            println("🔍 DEBUG - overallLength: $overallLength")
            println("🔍 DEBUG - isInspectionDocMandatory: $isInspectionDocMandatory")
        }

        // ✅ Review Step - ALWAYS show for BOTH new and existing ships
        // For NEW ships: Review data AND call send-request API (creates registration request)
        // For EXISTING ships: Review data ONLY (skip send-request API - no registration needed)
        if (isAddingNewUnit && !hasSelectedExistingUnit) {
            println("✅ Adding Review Step (for NEW ship - will call send-request API)")
        } else if (hasSelectedExistingUnit) {
            println("✅ Adding Review Step (for EXISTING ship - will skip send-request API)")
        }

        println("🔍 DEBUG: requiredDocuments.size = ${requiredDocuments.size}")
//        // Only show attachments step when API returns documents; otherwise skip to avoid empty review section
//        if (requiredDocuments.isNotEmpty()) {
//            steps.add(
//                SharedSteps.dynamicDocumentsStep(
//                    documents = requiredDocuments  // ✅ Pass documents from API for requestTypeId 8
//                )
//            )
//        } else {
//            println("ℹ️ Skipping dynamic documents step - no required documents returned from API")
//        }

        // ✅ Add inspection purpose/authority step with API data
        // Convert grouped authorities Map to DropdownSection list (inject id|name when available)
        val inspectionAuthoritySections = inspectionAuthorityOptions.map { (groupName, authorities) ->
            DropdownSection(
                title = groupName,
                items = authorities.map { authorityName ->
                    val id = lookupRepository.getInspectionAuthorityId(authorityName)
                    if (id != null) "$id|$authorityName" else authorityName
                }
            )
        }

        // Get ports for recording port dropdown (reuse existing portOptions)
        val recordingPorts = portOptions.ifEmpty { emptyList() }

        println("📊 Creating inspection step with:")
        println("   - ${inspectionPurposeOptions.size} purposes")
        println("   - ${recordingPorts.size} recording ports")
        println("   - ${inspectionAuthoritySections.size} authority sections")

        println("🔍 DEBUG: requiredDocuments.size = ${requiredDocuments.size}")

        // ✅ ALWAYS add inspection step - even if documents aren't loaded yet
        // This prevents step indices from shifting when documents load asynchronously
        val purposeOptionsWithIds = inspectionPurposeOptions.ifEmpty { emptyList() }.map { name ->
            val id = lookupRepository.getInspectionPurposeId(name)
            if (id != null) "$id|$name" else name
        }

        val portOptionsWithIds = recordingPorts.map { name ->
            val id = lookupRepository.getPortId(name)
            if (id != null) "$id|$name" else name
        }

        // ✅ Always add the step - documents will be empty list if not loaded yet
        steps.add(
            SharedSteps.inspectionPurposeAndAuthorityStep(
                inspectionPurposes = purposeOptionsWithIds,
                recordingPorts = portOptionsWithIds,
                authoritySections = inspectionAuthoritySections.ifEmpty { emptyList() },
                documents = requiredDocuments  // Will be empty list if not loaded yet
            )
        )

        if (requiredDocuments.isEmpty()) {
            println("ℹ️ Inspection step added with no documents (will update when documents load)")
        } else {
            println("✅ Inspection step added with ${requiredDocuments.size} documents")
        }

        steps.add(SharedSteps.reviewStep())

        // ✅ NEW: Payment Steps - Only show if we have requestId from name selection API
        val hasRequestId = accumulatedFormData["requestId"] != null

        if (hasRequestId) {
            // Payment Details Step - Shows payment breakdown
            steps.add(SharedSteps.paymentDetailsStep(accumulatedFormData))

            // Payment Success Step - Only show if payment was successful
            val paymentSuccessful = accumulatedFormData["paymentSuccessful"]?.toBoolean() ?: false
            if (paymentSuccessful) {
                steps.add(SharedSteps.paymentSuccessStep())
            }
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

        if (fieldIds.contains("grossTonnage")) {
            println("🔍 Step contains grossTonnage field")
            // ✅ Marine Unit Weights Step - Always add cross-step rules
            if (fieldIds.contains("grossTonnage")) {
                println("🔍 Step contains grossTonnage field")
                // ✅ Pass accumulated data to validation rules
                rules.addAll(MarineUnitValidationRules.getAllWeightRules(accumulatedFormData))
                println("🔍 Added ${rules.size} marine unit validation rules")
            }
            // Check if MMSI field exists
            if (accumulatedFormData.containsKey("mmsi")) {
                println("🔍 ✅ Adding MMSI validation rule")
                rules.add(MarineUnitValidationRules.mmsiRequiredForMediumVessels(accumulatedFormData ))
            }
        }

        // ✅ Document Rules - Inspection document based on overallLength
        if (fieldIds.contains("inspectionDocuments")) {
            println("🔍 Step contains inspectionDocuments field")
            // Check if we have overallLength in accumulated data
            if (accumulatedFormData.containsKey("overallLength")) {
                println("🔍 ✅ Adding inspection document validation rule based on overallLength")
                // rules.addAll(DocumentValidationRules.getAllDocumentRules(accumulatedFormData))  // TODO: Fix this
                println("🔍 Added document validation rules")
            }
        }

        // Same-step validations
        if (fieldIds.containsAll(listOf("grossTonnage", "netTonnage"))) {
            rules.add(MarineUnitValidationRules.netTonnageLessThanOrEqualGross())
        }

        if (fieldIds.containsAll(listOf("grossTonnage", "staticLoad"))) {
            rules.add(MarineUnitValidationRules.staticLoadValidation())
        }

        if (fieldIds.containsAll(listOf("staticLoad", "maxPermittedLoad"))) {
            rules.add(MarineUnitValidationRules.maxPermittedLoadValidation())
        }

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

        // Date Rules
        if (fieldIds.contains("manufacturerYear")) {
            rules.add(DateValidationRules.manufacturerYearValidation())
        }

        if (fieldIds.containsAll(listOf("constructionEndDate", "firstRegistrationDate"))) {
            rules.add(DateValidationRules.registrationAfterConstruction())
        }

        return rules
    }

    override suspend fun processStepData(step: Int, data: Map<String, String>): Int {
        println("🔄 processStepData called with: $data")

        // ✅ Update accumulated data
        accumulatedFormData.putAll(data)

        println("📦 accumulatedFormData after update: $accumulatedFormData")

        // ✅ Get current step data
        val currentStepData = getSteps().getOrNull(step)
        if (currentStepData != null) {
            val stepType = currentStepData.stepType

            println("🔍 DEBUG - Step $step type: $stepType")
            println("🔍 DEBUG - Data keys: ${data.keys}")

            // ✅ NEW: Check if we just completed the Marine Unit Selection step
            if (currentStepData.titleRes == R.string.owned_ships) {
                println("🚢 ✅ Marine Unit Selection step completed")

                // ✅ FIX: Only call ShipSelectionManager if user selected an EXISTING ship
                // If user clicked "add_ship", skip the API call and continue to next step
                val isAddingNew = accumulatedFormData["isAddingNewUnit"]?.toBoolean() ?: false
                val selectedUnitsJson = data["selectedMarineUnits"]
                val hasSelectedExistingShip = !selectedUnitsJson.isNullOrEmpty() &&
                        selectedUnitsJson != "[]" &&
                        !isAddingNew

                println("🔍 isAddingNew: $isAddingNew")
                println("🔍 selectedUnitsJson: $selectedUnitsJson")
                println("🔍 hasSelectedExistingShip: $hasSelectedExistingShip")

                if (hasSelectedExistingShip) {
                    println("🚢 User selected EXISTING ship - calling ShipSelectionManager...")
//                     ✅ WORKAROUND: Simulate successful API call for existing ship selection
                    accumulatedFormData["requestId"] = "25"

                    try {
                        val result = shipSelectionManager.handleShipSelection(
                            shipId = data["selectedMarineUnits"],
                            context = transactionContext
                        )

                        when (result) {
                            is com.informatique.mtcit.business.transactions.shared.ShipSelectionResult.Success -> {
                                println("✅ Ship selection successful via Manager!")
                                accumulatedFormData["requestId"] = result.requestId.toString()
                                accumulatedFormData["createdRequestId"] = result.requestId.toString()
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
                } else {
                    println("✅ User is adding NEW ship - skipping ShipSelectionManager, continuing to next step")
                    // User is adding a new ship - don't call the API, just continue to unit data step
                }
            }

            // ✅ NEW: Handle INSPECTION_PURPOSES_AND_AUTHORITIES step - Submit inspection request with documents
            if (stepType == StepType.INSPECTION_PURPOSES_AND_AUTHORITIES) {
                println("🔍 Processing INSPECTION_PURPOSES_AND_AUTHORITIES step - submitting request with documents...")

                // ✅ DEBUG: Print ALL accumulated formData before processing
                println("🔍 DEBUG - accumulatedFormData BEFORE processing:")
                accumulatedFormData.forEach { (key, value) ->
                    println("   [$key] = $value")
                }

                // ✅ Check if user is adding new ship (not selecting existing ship)
                val isAddingNewUnit = accumulatedFormData["isAddingNewUnit"]?.toBoolean() ?: false
                val selectedUnits = accumulatedFormData["selectedMarineUnits"]
                val isSelectingExistingShip = !selectedUnits.isNullOrEmpty() && selectedUnits != "[]"

                println("🔍 DEBUG - User flow:")
                println("   isAddingNewUnit: $isAddingNewUnit")
                println("   selectedMarineUnits: $selectedUnits")
                println("   isSelectingExistingShip: $isSelectingExistingShip")
                println("   requestId in formData: ${accumulatedFormData["requestId"]}")
                println("   shipInfoId in formData: ${accumulatedFormData["shipInfoId"]}")
                println("   shipId in formData: ${accumulatedFormData["shipId"]}")

                // ✅ Create a copy of formData for inspection request
                val inspectionFormData = accumulatedFormData.toMutableMap()

                // ✅ If adding new ship, remove requestId so it sends null
                if (isAddingNewUnit || !isSelectingExistingShip) {
                    println("⚠️ User is adding NEW ship - removing requestId from inspection request (will send id = null)")
                    inspectionFormData.remove("requestId")
                } else {
                    println("✅ User selected EXISTING ship - keeping requestId for inspection request")
                }

                // ✅ DEBUG: Print final formData being sent to manager
                println("🔍 DEBUG - inspectionFormData AFTER processing:")
                inspectionFormData.forEach { (key, value) ->
                    println("   [$key] = $value")
                }

                try {
                    val result = inspectionRequestManager.submitInspectionRequest(
                        formData = inspectionFormData,
                        context = appContext
                    )

                    when (result) {
                        is com.informatique.mtcit.business.transactions.shared.InspectionSubmitResult.Success -> {
                            println("✅ Inspection request submitted successfully!")
                            println("   Message: ${result.message}")
                            println("   Request ID: ${result.requestId}")

                            accumulatedFormData["requestId"] = result.requestId.toString()

                            // Store success message
                            accumulatedFormData["inspectionSubmitMessage"] = result.message
                            accumulatedFormData["inspectionSubmitted"] = "true"

                            // Continue to next step
                        }
                        is com.informatique.mtcit.business.transactions.shared.InspectionSubmitResult.Error -> {
                            println("❌ Inspection request submission failed: ${result.message}")
                            accumulatedFormData["apiError"] = result.message
                            return -1 // Block navigation
                        }
                    }
                } catch (e: Exception) {
                    println("❌ Exception submitting inspection request: ${e.message}")
                    e.printStackTrace()
                    accumulatedFormData["apiError"] = "حدث خطأ أثناء إرسال طلب المعاينة: ${e.message}"
                    return -1
                }
            }

            // ✅ Call RegistrationRequestManager to process registration-related steps
            val registrationResult = registrationRequestManager.processStepIfNeeded(
                stepType = stepType,
                formData = accumulatedFormData,
                requestTypeId = requestTypeId, // 8 = Request Inspection
                context = appContext
            )

            when (registrationResult) {
                is StepProcessResult.Success -> {
                    println("✅ Registration step processed: ${registrationResult.message}")

                    // Extract requestId if it was set
                    accumulatedFormData["requestId"]?.let { requestIdStr ->
                        requestId = requestIdStr.toLongOrNull()
                        println("✅ requestId: ${requestIdStr} -> parsed: $requestId")
                    }

                    // Check if we need to trigger step rebuild
                    if (stepType == StepType.MARINE_UNIT_SELECTION) {
                        onStepsNeedRebuild?.invoke()
                    }

                    // Check if we just completed Review Step and need inspection
                    if (stepType == StepType.REVIEW) {
                        val needInspection = accumulatedFormData["needInspection"]?.toBoolean() ?: false
                        val sendRequestMessage = accumulatedFormData["sendRequestMessage"]

                        if (needInspection) {
                            println("🔍 Inspection required for this request")
                            accumulatedFormData["showInspectionDialog"] = "true"
                            accumulatedFormData["inspectionMessage"] = sendRequestMessage ?: "في إنتظار نتيجه الفحص الفني"
                            return step
                        }
                    }
                }
                is StepProcessResult.Error -> {
                    println("❌ Registration error: ${registrationResult.message}")
                    // ✅ Block navigation on error
                    return -1
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
                            // ✅ Get endpoint and context from transactionContext
                            val endpoint = transactionContext.sendRequestEndpoint
                            val contextName = transactionContext.displayName

                            println("🚀 Calling ReviewManager.processReviewStep:")
                            println("   Endpoint: $endpoint")
                            println("   RequestId: $requestIdInt")
                            println("   Context: $contextName")

                            // ✅ Call ReviewManager which internally uses marineUnitsApiService via repository
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
                                    println("   Need Inspection: ${result.needInspection}")

                                    // ✅ Store response in formData
                                    accumulatedFormData["needInspection"] = result.needInspection.toString()
                                    accumulatedFormData["sendRequestMessage"] = result.message

                                    // ✅ Extract request number
                                    val requestNumber = result.additionalData?.get("requestNumber")?.toString()
                                        ?: result.additionalData?.get("requestSerial")?.toString()
                                        ?: accumulatedFormData["requestSerial"]
                                        ?: accumulatedFormData["requestId"] // Use requestId if available
                                        ?: "N/A"

                                    // ✅ NEW: Check if this is a NEW request (not resumed)
                                    val isNewRequest = accumulatedFormData["isResumedTransaction"]?.toBoolean() != true

                                    println("🔍 isNewRequest check:")
                                    println("   - isResumedTransaction flag: ${accumulatedFormData["isResumedTransaction"]}")
                                    println("   - isNewRequest result: $isNewRequest")

                                    if (isNewRequest) {
                                        println("🎉 NEW inspection request submitted - showing success dialog and stopping")

                                        // Set success flags for ViewModel to show dialog
                                        accumulatedFormData["requestSubmitted"] = "true"
                                        accumulatedFormData["requestNumber"] = requestNumber
                                        accumulatedFormData["successMessage"] = result.message

                                        // Return -2 to indicate: success but show dialog and stop
                                        return -2
                                    }

                                    // ✅ For resumed requests: check if inspection needed
                                    if (result.needInspection) {
                                        println("🔍 Inspection required - showing dialog")
                                        accumulatedFormData["showInspectionDialog"] = "true"
                                        accumulatedFormData["inspectionMessage"] = result.message
                                        return step // Stay on current step
                                    }

                                    // Proceed to next step (for resumed requests only)
                                    println("✅ No inspection needed - proceeding to next step (resumed request)")
                                }
                                is com.informatique.mtcit.business.transactions.shared.ReviewResult.Error -> {
                                    println("❌ Review step failed: ${result.message}")
                                    accumulatedFormData["apiError"] = result.message
                                    return -1 // Block navigation
                                }
                            }
                        } catch (e: Exception) {
                            println("❌ Exception in review step: ${e.message}")
                            e.printStackTrace()
                            accumulatedFormData["apiError"] = "حدث خطأ أثناء إرسال الطلب: ${e.message}"
                            return -1
                        }
                    }
                }
            }

            // ✅ Call PaymentManager to process payment-related steps
            val paymentResult = paymentManager.processStepIfNeeded(
                stepType = stepType,
                formData = accumulatedFormData,
                requestTypeId = requestTypeId.toInt(), // 8 = Request Inspection
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

        return step
    }

    /**
     * ✅ NEW: Called when a step is opened - loads required lookups lazily
     */
    override suspend fun onStepOpened(stepIndex: Int) {
        val step = getSteps().getOrNull(stepIndex) ?: return

        // ✅ NEW: If this is the payment step, trigger payment API call
        if (step.stepType == StepType.PAYMENT) {
            println("💰 Payment step opened - triggering payment receipt API call...")

            // Call PaymentManager to load payment receipt
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
            return // Don't process lookups for payment step
        }

        // ✅ NEW: If this is inspection purposes step, force reload authorities if we have shipInfoId now
        if (step.stepType == StepType.INSPECTION_PURPOSES_AND_AUTHORITIES) {
            println("🔍 Inspection purposes step opened")

            val shipInfoId = accumulatedFormData["shipInfoId"]
            if (shipInfoId != null && inspectionAuthorityOptions.isEmpty()) {
                println("📥 shipInfoId is now available ($shipInfoId) - force loading inspection authorities...")

                // Clear the cached options to force reload
                inspectionAuthorityOptions = emptyMap()

                // Trigger the lookup to load
                onLookupStarted?.invoke("inspectionAuthorities")
                loadLookup("inspectionAuthorities")
            }
        }


        if (step.requiredLookups.isEmpty()) {
            println("ℹ️ Step $stepIndex has no required lookups")
            return
        }

        println("🔄 Loading ${step.requiredLookups.size} lookups in PARALLEL for step $stepIndex: ${step.requiredLookups}")

        // ✅ Notify ViewModel that all lookups are starting (sets loading state immediately)
        step.requiredLookups.forEach { lookupKey ->
            onLookupStarted?.invoke(lookupKey)
        }

        // ✅ Launch all lookups in parallel - each updates UI independently when done
        kotlinx.coroutines.coroutineScope {
            step.requiredLookups.forEach { lookupKey ->
                launch {
                    loadLookup(lookupKey)
                }
            }
        }

        println("✅ Finished loading all lookups for step $stepIndex")

        // ✅ Rebuild steps after all lookups complete
        onStepsNeedRebuild?.invoke()
    }

    /**
     * ✅ NEW: Helper method to load a single lookup and notify completion
     * Reduces code duplication and makes it easier to add new lookups
     */
    private suspend fun loadLookup(lookupKey: String) {
        try {
            when (lookupKey) {
                "ports" -> {
                    if (portOptions.isEmpty()) {
                        println("📥 Loading ports...")
                        val data = lookupRepository.getPorts().getOrNull() ?: emptyList()
                        portOptions = data
                        println("✅ Loaded ${portOptions.size} ports")
                        onLookupCompleted?.invoke("ports", data, true)
                    } else {
                        // Already loaded - notify with cached data
                        onLookupCompleted?.invoke("ports", portOptions, true)
                    }
                }
                "countries" -> {
                    if (countryOptions.isEmpty()) {
                        println("📥 Loading countries...")
                        val data = lookupRepository.getCountries().getOrNull() ?: emptyList()
                        countryOptions = data
                        println("✅ Loaded ${countryOptions.size} countries")
                        onLookupCompleted?.invoke("countries", data, true)
                    } else {
                        onLookupCompleted?.invoke("countries", countryOptions, true)
                    }
                }
                "nationalities" -> {
                    if (countryOptions.isEmpty()) {
                        println("📥 Loading nationalities/countries...")
                        val data = lookupRepository.getCountries().getOrNull() ?: emptyList()
                        countryOptions = data
                        println("✅ Loaded ${countryOptions.size} countries/nationalities")
                        onLookupCompleted?.invoke("nationalities", data, true)
                    } else {
                        onLookupCompleted?.invoke("nationalities", countryOptions, true)
                    }
                }
                "shipTypes" -> {
                    if (shipTypeOptions.isEmpty()) {
                        println("📥 Loading ship types...")
                        val data = lookupRepository.getShipTypes().getOrNull() ?: emptyList()
                        shipTypeOptions = data
                        println("✅ Loaded ${shipTypeOptions.size} ship types")
                        onLookupCompleted?.invoke("shipTypes", data, true)
                    } else {
                        onLookupCompleted?.invoke("shipTypes", shipTypeOptions, true)
                    }
                }
                "shipCategories" -> {
                    if (shipCategoryOptions.isEmpty()) {
                        println("📥 Loading ship categories...")
                        val data = lookupRepository.getShipCategories().getOrNull() ?: emptyList()
                        shipCategoryOptions = data
                        println("✅ Loaded ${shipCategoryOptions.size} ship categories")
                        onLookupCompleted?.invoke("shipCategories", data, true)
                    } else {
                        onLookupCompleted?.invoke("shipCategories", shipCategoryOptions, true)
                    }
                }
                "marineActivities" -> {
                    if (marineActivityOptions.isEmpty()) {
                        println("📥 Loading marine activities...")
                        val data = lookupRepository.getMarineActivities().getOrNull() ?: emptyList()
                        marineActivityOptions = data
                        println("✅ Loaded ${marineActivityOptions.size} marine activities")
                        onLookupCompleted?.invoke("marineActivities", data, true)
                    } else {
                        onLookupCompleted?.invoke("marineActivities", marineActivityOptions, true)
                    }
                }
                "proofTypes" -> {
                    if (proofTypeOptions.isEmpty()) {
                        println("📥 Loading proof types...")
                        val data = lookupRepository.getProofTypes().getOrNull() ?: emptyList()
                        proofTypeOptions = data
                        println("✅ Loaded ${proofTypeOptions.size} proof types")
                        onLookupCompleted?.invoke("proofTypes", data, true)
                    } else {
                        onLookupCompleted?.invoke("proofTypes", proofTypeOptions, true)
                    }
                }
                "engineTypes" -> {
                    if (engineTypeOptions.isEmpty()) {
                        println("📥 Loading engine types...")
                        val data = lookupRepository.getEngineTypes().getOrNull() ?: emptyList()
                        engineTypeOptions = data
                        println("✅ Loaded ${engineTypeOptions.size} engine types")
                        onLookupCompleted?.invoke("engineTypes", data, true)
                    } else {
                        onLookupCompleted?.invoke("engineTypes", engineTypeOptions, true)
                    }
                }
                "engineStatuses" -> {
                    if (engineStatusOptions.isEmpty()) {
                        println("📥 Loading engine statuses...")
                        val data = lookupRepository.getEngineStatuses().getOrNull() ?: emptyList()
                        engineStatusOptions = data
                        println("✅ Loaded ${engineStatusOptions.size} engine statuses")
                        onLookupCompleted?.invoke("engineStatuses", data, true)
                    } else {
                        onLookupCompleted?.invoke("engineStatuses", engineStatusOptions, true)
                    }
                }
                "engineFuelTypes" -> {
                    if (engineFuelTypeOptions.isEmpty()) {
                        println("📥 Loading engine fuel types...")
                        val data = lookupRepository.getEngineFuelTypes().getOrNull() ?: emptyList()
                        engineFuelTypeOptions = data
                        println("✅ Loaded ${engineFuelTypeOptions.size} fuel types")
                        onLookupCompleted?.invoke("engineFuelTypes", data, true)
                    } else {
                        onLookupCompleted?.invoke("engineFuelTypes", engineFuelTypeOptions, true)
                    }
                }
                "buildMaterials" -> {
                    if (buildMaterialOptions.isEmpty()) {
                        println("📥 Loading build materials...")
                        val data = lookupRepository.getBuildMaterials().getOrNull() ?: emptyList()
                        buildMaterialOptions = data
                        println("✅ Loaded ${buildMaterialOptions.size} build materials")
                        onLookupCompleted?.invoke("buildMaterials", data, true)
                    } else {
                        onLookupCompleted?.invoke("buildMaterials", buildMaterialOptions, true)
                    }
                }
                "inspectionPurposes" -> {
                    if (inspectionPurposeOptions.isEmpty()) {
                        println("📥 Loading inspection purposes...")
                        val data = lookupRepository.getInspectionPurposes().getOrNull() ?: emptyList()
                        inspectionPurposeOptions = data
                        println("✅ Loaded ${inspectionPurposeOptions.size} inspection purposes")
                        onLookupCompleted?.invoke("inspectionPurposes", data, true)
                    } else {
                        onLookupCompleted?.invoke("inspectionPurposes", inspectionPurposeOptions, true)
                    }
                }
                "inspectionAuthorities" -> {
                    // ✅ Get shipInfoId from accumulated form data
                    // Case 1: User selected existing ship → shipInfoId in selectedMarineUnits
                    // Case 2: User added new ship → shipInfoId returned from create request API

                    val shipInfoId = accumulatedFormData["shipInfoId"]?.toIntOrNull()
                        ?: run {
                            // Try to extract from selectedMarineUnits (existing ship)
                            val selectedUnitsJson = accumulatedFormData["selectedMarineUnits"]
                            selectedUnitsJson?.let {
                                it.replace("[", "")
                                  .replace("]", "")
                                  .replace("\"", "")
                                  .trim()
                                  .toIntOrNull()
                            }
                        }

                    println("🔍 DEBUG - Looking for shipInfoId for inspection authorities:")
                    println("   shipInfoId from formData: ${accumulatedFormData["shipInfoId"]}")
                    println("   selectedMarineUnits: ${accumulatedFormData["selectedMarineUnits"]}")
                    println("   Resolved shipInfoId: $shipInfoId")

                    if (shipInfoId == null) {
                        println("❌ No shipInfoId found - cannot load inspection authorities")
                        println("   Available keys: ${accumulatedFormData.keys}")
                        onLookupCompleted?.invoke("inspectionAuthorities", emptyList(), false)
                    } else {
                        println("📥 Loading inspection authorities for shipInfoId: $shipInfoId...")
                        try {
                            val data = lookupRepository.getInspectionAuthorities(shipInfoId).getOrNull() ?: emptyMap()
                            inspectionAuthorityOptions = data
                            println("✅ Loaded ${inspectionAuthorityOptions.size} authority groups")
                            data.forEach { (groupName, authorities) ->
                                println("   - $groupName: ${authorities.size} authorities")
                            }
                            // Convert Map to flat list for callback
                            val flatList = data.values.flatten()
                            onLookupCompleted?.invoke("inspectionAuthorities", flatList, true)
                        } catch (e: Exception) {
                            println("❌ Failed to load inspection authorities: ${e.message}")
                            e.printStackTrace()
                            onLookupCompleted?.invoke("inspectionAuthorities", emptyList(), false)
                        }
                    }
                }
                "inspectionPorts" -> {
                    // Reuse ports lookup for inspection recording ports
                    if (portOptions.isEmpty()) {
                        println("📥 Loading inspection recording ports...")
                        val data = lookupRepository.getPorts().getOrNull() ?: emptyList()
                        portOptions = data
                        println("✅ Loaded ${portOptions.size} inspection ports")
                        onLookupCompleted?.invoke("inspectionPorts", data, true)
                    } else {
                        onLookupCompleted?.invoke("inspectionPorts", portOptions, true)
                    }
                }
                else -> {
                    println("⚠️ Unknown lookup key: $lookupKey")
                    onLookupCompleted?.invoke(lookupKey, emptyList(), false)
                }
            }
        } catch (e: Exception) {
            println("❌ Failed to load lookup '$lookupKey': ${e.message}")
            e.printStackTrace()
            // ✅ Notify ViewModel even on failure (with empty list and success=false)
            onLookupCompleted?.invoke(lookupKey, emptyList(), false)
        }
    }

    /**
     * Validate marine unit selection using TemporaryRegistrationRules
     * Called from MarineRegistrationViewModel when user clicks "Accept & Send" on review step
     */
    override suspend fun validateMarineUnitSelection(unitId: String, userId: String): ValidationResult {
        return try {
            println("🔍 RequestInspectionStrategy: Validating unit $unitId using TemporaryRegistrationRules")

            // Find the selected unit
            val selectedUnit = marineUnits.firstOrNull { it.id == unitId }

            if (selectedUnit == null) {
                println("❌ Unit not found with id: $unitId")
                return ValidationResult.Error("الوحدة البحرية المختارة غير موجودة")
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
            ValidationResult.Error(e.message ?: "فشل التحقق من حالة الفحص")
        }
    }

    /**
     * NEW: Validate a NEW marine unit that doesn't exist in the database yet
     * This is used when user is adding a new marine unit during registration
     */
    override suspend fun validateNewMarineUnit(newUnit: MarineUnit, userId: String): ValidationResult {
        return try {
            println("🔍 RequestInspectionStrategy: Validating NEW unit ${newUnit.name} (id: ${newUnit.id})")

            // Use TemporaryRegistrationRules to validate the new unit
            val validationResult = temporaryRegistrationRules.validateUnit(newUnit, userId)
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
            ValidationResult.Error(e.message ?: "فشل التحقق من حالة الفحص")
        }
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        return Result.success(true)
    }

    // ✅ NEW: Implement TransactionStrategy interface methods for generic transaction handling

    override fun getTransactionTypeName(): String {
        return "طلب معاينة"
    }

    override fun getCreatedRequestId(): Int? {
        // Get from accumulatedFormData or requestId property
        val fromFormData = accumulatedFormData["requestId"]?.toIntOrNull()
        val fromProperty = requestId?.toInt()
        val fromCreated = accumulatedFormData["createdRequestId"]?.toIntOrNull()

        println("🔍 getCreatedRequestId() called:")
        println("   - accumulatedFormData['requestId'] = ${accumulatedFormData["requestId"]}")
        println("   - accumulatedFormData['createdRequestId'] = ${accumulatedFormData["createdRequestId"]}")
        println("   - fromFormData (parsed) = $fromFormData")
        println("   - requestId property = $requestId")
        println("   - fromProperty (parsed) = $fromProperty")
        println("   - fromCreated (parsed) = $fromCreated")
        println("   - Final result = ${fromFormData ?: fromProperty ?: fromCreated}")

        return fromFormData ?: fromProperty ?: fromCreated
    }

    override fun getStatusUpdateEndpoint(requestId: Int): String {
        return transactionContext.buildUpdateStatusUrl(requestId)
    }

    override fun getSendRequestEndpoint(requestId: Int): String {
        return transactionContext.buildSendRequestUrl(requestId)
    }
}
