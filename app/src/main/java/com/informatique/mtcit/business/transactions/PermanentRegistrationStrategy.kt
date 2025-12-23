package com.informatique.mtcit.business.transactions

import android.content.Context
import com.informatique.mtcit.R
import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.transactions.shared.DocumentConfig
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
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.data.repository.MarineUnitRepository
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
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
    private val shipSelectionManager: com.informatique.mtcit.business.transactions.shared.ShipSelectionManager,
    @ApplicationContext private val appContext: Context  // ✅ Injected context
) : TransactionStrategy {

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

    // NEW: Store filtered ship types based on selected category
    private var filteredShipTypeOptions: List<String> = emptyList()
    private var isShipTypeFiltered: Boolean = false

    // ✅ الحل: اعمل cache للـ form data
    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf()

    private val requestTypeId = TransactionType.PERMANENT_REGISTRATION_CERTIFICATE.toRequestTypeId()


    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        println("🔄 Loading ESSENTIAL lookups only (lazy loading enabled for step-specific lookups)...")

        // ✅ Load only ESSENTIAL lookups needed for initial steps
        // Step-specific lookups (ports, countries, ship types, etc.) will be loaded lazily via onStepOpened()

        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()
        val commercialRegistrations = lookupRepository.getCommercialRegistrations("12345678901234").getOrNull() ?: emptyList()

        // Store in instance variables
        typeOptions = personTypes
        commercialOptions = commercialRegistrations

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

        println("🚢 loadShipsForSelectedType called - personType=$personType, commercialReg=$commercialReg")

        // ✅ UPDATED: For companies, use commercialReg (crNumber) from selectionData
        // For individuals, use ownerCivilId
        val (ownerCivilId, commercialRegNumber) = when (personType) {
            "فرد" -> {
                println("✅ Individual: Using ownerCivilId")
                Pair("12345678", null)
            }
            "شركة" -> {
                println("✅ Company: Using commercialRegNumber from selectionData = $commercialReg")
                Pair("12345678", commercialReg) // ✅ Send both ownerCivilId AND commercialRegNumber
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

        return marineUnits
    }

    override suspend fun clearLoadedShips() {
        println("🧹 Clearing loaded ships cache")
        marineUnits = emptyList()
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
        TODO("Not yet implemented")
    }

    override fun getSteps(): List<StepData> {
        val steps = mutableListOf<StepData>()

        steps.add(SharedSteps.personTypeStep(typeOptions))

        // Step 1: Commercial Registration (بس لو اختار شركة)
        val selectedPersonType = accumulatedFormData["selectionPersonType"]

        if (selectedPersonType == "شركة") {  // ⚠️ بيقارن بالـ string "شركة"
            steps.add(SharedSteps.commercialRegistrationStep(commercialOptions))
        }

        steps.add(
            SharedSteps.marineUnitRegistrationCertificateStep(
                showInfoMessage = true
            )
        )
        val hasTemporaryCertificate = accumulatedFormData["hasTemporaryRegistrationCertificate"]

        if (hasTemporaryCertificate == "yes") {
            // ✅ لو "نعم": اتخطى كل الـ steps التقنية وروح على Insurance مباشرة
            println("✅ User has temporary certificate - Skipping technical steps")
            // بس هنضيف Owner Info (مهم للتسجيل)


            steps.add(
                SharedSteps.marineUnitSelectionStep(
                    units = marineUnits,
                    allowMultipleSelection = false, // اختيار وحدة واحدة فقط
                    showAddNewButton = false,
                    showOwnedUnitsWarning = true
                )
            )
            steps.add(
                SharedSteps.insuranceDocumentStep(
                    countries = countryOptions
                )
            )
            steps.add(
                SharedSteps.marineUnitNameSelectionStep(
                    showReservationInfo = true
                )
            )

        } else if (hasTemporaryCertificate == "no") {

            // Use filtered ship types if available, otherwise use empty list
            val shipTypesToUse = if (isShipTypeFiltered) filteredShipTypeOptions else emptyList()

            println("🔧 getSteps - isFiltered: $isShipTypeFiltered, types count: ${shipTypesToUse.size}")

            steps.add(
                SharedSteps.unitSelectionStep(
                    shipTypes = shipTypesToUse,  // Use filtered types or empty list
                    shipCategories = shipCategoryOptions,
                    ports = portOptions,
                    countries = countryOptions,
                    marineActivities = marineActivityOptions,
                    proofTypes = proofTypeOptions,
                    buildingMaterials = emptyList(), // TODO: Add when API ready
                    includeIMO = true,
                    includeMMSI = true,
                    includeManufacturer = true,
                    includeProofDocument = false,
                    includeConstructionDates = true,
                    includeRegistrationCountry = true
                )
            )

            steps.add(
                SharedSteps.marineUnitDimensionsStep(
                    includeHeight = true,
                    includeDecksCount = true
                )
            )

            steps.add(
                SharedSteps.marineUnitWeightsStep(
                    includeMaxPermittedLoad = true
                )
            )

            steps.add(
                SharedSteps.engineInfoStep(
                    manufacturers = listOf(
                        "Manufacturer 1",
                        "Manufacturer 2",
                        "Manufacturer 3"
                    ),
                    enginesTypes = engineTypeOptions,
                    countries = countryOptions,
                    fuelTypes = engineFuelTypeOptions,
                    engineConditions = engineStatusOptions,
                )
            )
//            }


            steps.add(
                SharedSteps.ownerInfoStep(
                    nationalities = countryOptions,
                    countries = countryOptions,
                    includeCompanyFields = true,
                )
            )
            steps.add(
                SharedSteps.documentsStep(
                    requiredDocuments = listOf(
                        DocumentConfig(
                            id = "shipbuildingCertificate",
                            labelRes = R.string.shipbuilding_certificate_or_sale_contract,
                            mandatory = true
                        ),
                        DocumentConfig(
                            id = "inspectionDocuments",
                            labelRes = R.string.inspection_documents,
                            mandatory = true
                        )
                    )
                )
            )
            steps.add(SharedSteps.reviewStep())
            steps.add(
                SharedSteps.insuranceDocumentStep(
                    countries = countryOptions
                )
            )
        }
            steps.add(
                SharedSteps.marineUnitNameSelectionStep(
                    showReservationInfo = true
                )
            )

        return steps
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
                            // ✅ Get endpoint and context from transactionContext
                            val transactionContext = TransactionType.PERMANENT_REGISTRATION_CERTIFICATE.context
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

                                    // ✅ Store response in formData
                                    accumulatedFormData["sendRequestMessage"] = reviewResult.message

                                    // ✅ PERMANENT REGISTRATION: Different response handling than temporary
                                    // For permanent registration, we might check for different fields
                                    // e.g., approvalStatus, documentVerification, etc.

                                    // Check additionalData for permanent-specific fields
                                    val approvalRequired = reviewResult.additionalData?.get("approvalRequired") as? Boolean
                                    val documentVerification = reviewResult.additionalData?.get("documentVerification") as? String

                                    if (approvalRequired == true) {
                                        println("⚠️ Approval required for permanent registration")
                                        accumulatedFormData["showApprovalDialog"] = "true"
                                        accumulatedFormData["approvalMessage"] = reviewResult.message
                                        return step // Stay on current step
                                    }

                                    if (documentVerification == "pending") {
                                        println("📄 Document verification pending")
                                        accumulatedFormData["showDocVerificationDialog"] = "true"
                                        accumulatedFormData["verificationMessage"] = reviewResult.message
                                        return step // Stay on current step
                                    }

                                    // ✅ Also support needInspection (common field)
                                    if (reviewResult.needInspection) {
                                        println("🔍 Inspection required - showing dialog")
                                        accumulatedFormData["showInspectionDialog"] = "true"
                                        accumulatedFormData["inspectionMessage"] = reviewResult.message
                                        return step // Stay on current step
                                    }

                                    // Proceed to next step
                                    println("✅ No blocking conditions - proceeding to next step")
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
                            accumulatedFormData["apiError"] = "حدث خطأ أثناء إرسال الطلب: ${e.message}"
                            return -1
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
}
