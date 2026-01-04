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
import com.informatique.mtcit.common.FormField
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
    private var insuranceCompanyOptions: List<String> = emptyList() // ✅ Add insurance companies
    private var requiredDocuments: List<com.informatique.mtcit.data.model.RequiredDocumentItem> = emptyList() // ✅ Store required documents

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

    // ✅ Allow ViewModel to set a callback when steps need to be rebuilt
    override var onStepsNeedRebuild: (() -> Unit)? = null


    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        println("🔄 Loading ESSENTIAL lookups only (lazy loading enabled for step-specific lookups)...")

        // ✅ Load only ESSENTIAL lookups needed for initial steps
        // Step-specific lookups (ports, countries, ship types, etc.) will be loaded lazily via onStepOpened()

        // ✅ Get civilId from token
        // ✅ Get civilId from token
        val ownerCivilId = UserHelper.getOwnerCivilId(appContext)
        println("🔑 Owner CivilId from token: $ownerCivilId")

        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()
        val commercialRegistrations = lookupRepository.getCommercialRegistrations(ownerCivilId).getOrNull() ?: emptyList()

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
        TODO("Not yet implemented")
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

        // ✅ Step 6: Payment Details - Show if we have a request ID
        val hasRequestId = accumulatedFormData["requestId"] != null
        if (hasRequestId) {
            steps.add(SharedSteps.paymentDetailsStep(accumulatedFormData))
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
                            accumulatedFormData["requestId"] = result.requestId.toString()
                            requestId = result.requestId.toLong()

                            // ✅ NEW: Store maritime identification data
                            selectedShipImoNumber = result.imoNumber
                            selectedShipMmsiNumber = result.mmsiNumber
                            selectedShipCallSign = result.callSign
                            needsMaritimeIdentification = result.needsMaritimeIdentification

                            // ✅ Store shipId from the API response (needed for maritime identity API)
                            result.shipId?.let {
                                accumulatedFormData["shipId"] = it.toString()
                                println("✅ Stored shipId from API response: $it")
                            } ?: run {
                                // Fallback: use selectedMarineUnits if shipId not in response
                                val selectedShipId = data["selectedMarineUnits"]
                                if (selectedShipId != null) {
                                    accumulatedFormData["shipId"] = selectedShipId
                                    println("✅ Stored shipId from selectedMarineUnits: $selectedShipId")
                                }
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
            if (currentStepData?.stepType == StepType.INSURANCE_DOCUMENT) {
                println("📄 ✅ Insurance Document step completed - calling validate-insurance-document API...")

                try {
                    // ✅ Get ship ID - handle both array format and single value
                    val shipIdString = accumulatedFormData["shipId"]
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
                            println("❌ Failed to validate insurance document: ${error.message}")
                            val errorMsg =
                                com.informatique.mtcit.common.ErrorMessageExtractor.extract(error.message)
                            accumulatedFormData["apiError"] = errorMsg
                            throw ApiException(500, errorMsg)
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

                                        // ✅ Store response in formData
                                        accumulatedFormData["sendRequestMessage"] =
                                            reviewResult.message

                                        // ✅ PERMANENT REGISTRATION: Different response handling than temporary
                                        // For permanent registration, we might check for different fields
                                        // e.g., approvalStatus, documentVerification, etc.

                                        // Check additionalData for permanent-specific fields
                                        val approvalRequired =
                                            reviewResult.additionalData?.get("approvalRequired") as? Boolean
                                        val documentVerification =
                                            reviewResult.additionalData?.get("documentVerification") as? String

                                        if (approvalRequired == true) {
                                            println("⚠️ Approval required for permanent registration")
                                            accumulatedFormData["showApprovalDialog"] = "true"
                                            accumulatedFormData["approvalMessage"] =
                                                reviewResult.message
                                            return step // Stay on current step
                                        }

                                        if (documentVerification == "pending") {
                                            println("📄 Document verification pending")
                                            accumulatedFormData["showDocVerificationDialog"] =
                                                "true"
                                            accumulatedFormData["verificationMessage"] =
                                                reviewResult.message
                                            return step // Stay on current step
                                        }

                                        // ✅ Also support needInspection (common field)
                                        if (reviewResult.needInspection) {
                                            println("🔍 Inspection required - showing dialog")
                                            accumulatedFormData["showInspectionDialog"] = "true"
                                            accumulatedFormData["inspectionMessage"] =
                                                reviewResult.message
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
                                accumulatedFormData["apiError"] =
                                    "حدث خطأ أثناء إرسال الطلب: ${e.message}"
                                return -1
                            }
                        }

                        // ✅ HANDLE PAYMENT STEP
                        if (stepType == StepType.PAYMENT) {
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
                                    val showPaymentSuccessDialog = accumulatedFormData["showPaymentSuccessDialog"]?.toBoolean() ?: false
                                    if (showPaymentSuccessDialog) {
                                        println("✅ Payment submitted successfully - dialog will be shown")
                                        return step
                                    }
                                }
                                is StepProcessResult.Error -> {
                                    println("❌ Payment error: ${paymentResult.message}")
                                    accumulatedFormData["apiError"] = paymentResult.message
                                    return -1
                                }
                                is StepProcessResult.NoAction -> {
                                    println("ℹ️ No payment action needed")
                                }
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
}
