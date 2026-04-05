package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.R
import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.business.transactions.shared.StepType
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.ui.components.PersonType
import com.informatique.mtcit.ui.components.SelectableItem
import com.informatique.mtcit.ui.repo.CompanyRepo
import com.informatique.mtcit.ui.viewmodels.StepData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import com.informatique.mtcit.util.UserHelper
import com.informatique.mtcit.business.transactions.marineunit.rules.MortgageCertificateRules
import com.informatique.mtcit.business.transactions.marineunit.usecases.ValidateMarineUnitUseCase
import com.informatique.mtcit.business.transactions.marineunit.usecases.GetEligibleMarineUnitsUseCase
import com.informatique.mtcit.data.repository.MarineUnitRepository
import android.content.Context
import com.informatique.mtcit.business.transactions.shared.StepProcessResult
import com.informatique.mtcit.common.ApiException
import com.informatique.mtcit.data.api.MarineUnitsApiService
import com.informatique.mtcit.data.helpers.FileUploadHelper
import com.informatique.mtcit.data.model.OwnerFileUpload
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/**
 * Strategy for Mortgage Certificate Issuance
 * Steps:
 * 1. Person Type Selection (Individual/Company)
 * 2. Commercial Registration (conditional - only for Company)
 * 3. Unit Selection (choose from user's ships) - WITH BUSINESS VALIDATION
 * 4. Mortgage Data (bank info and mortgage details)
 * 5. Review
 */
class MortgageCertificateStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val companyRepository: CompanyRepo,
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository,
    private val mortgageRules: MortgageCertificateRules,
    private val validateMarineUnitUseCase: ValidateMarineUnitUseCase,
    private val getEligibleUnitsUseCase: GetEligibleMarineUnitsUseCase,
    private val marineUnitsApiService: MarineUnitsApiService,
    private val marineUnitRepository: MarineUnitRepository,
    private val mortgageApiService: com.informatique.mtcit.data.api.MortgageApiService,
    private val shipSelectionManager: com.informatique.mtcit.business.transactions.shared.ShipSelectionManager,
    private val reviewManager: com.informatique.mtcit.business.transactions.shared.ReviewManager,
    private val paymentManager: com.informatique.mtcit.business.transactions.shared.PaymentManager,
    @ApplicationContext private val appContext: Context
) : BaseTransactionStrategy() {

    private var portOptions: List<String> = emptyList()
    private var countryOptions: List<String> = emptyList()
    private var personTypeOptions: List<PersonType> = emptyList()
    private var commercialOptions: List<SelectableItem> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()
    private var mortgageReasons: List<String> = emptyList()
    private var banks: List<String> = emptyList()
    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf()

    // ✅ INFINITE SCROLL: pagination state
    private var _currentShipsPage: Int = -1
    private var _isLastShipsPage: Boolean = true
    override val currentShipsPage: Int get() = _currentShipsPage
    override val isLastShipsPage: Boolean get() = _isLastShipsPage

    // ✅ NEW: Store required documents from API
    private var requiredDocuments: List<com.informatique.mtcit.data.model.RequiredDocumentItem> = emptyList()
    private val requestTypeId = TransactionType.MORTGAGE_CERTIFICATE.toRequestTypeId()
    private val transactionContext: TransactionContext = TransactionType.MORTGAGE_CERTIFICATE.context

    // Store API error to prevent navigation and show error dialog
    private var lastApiError: String? = null

    // ✅ NEW: Store created mortgage request ID for status update
    private var createdMortgageRequestId: Int? = null

    // ✅ NEW: Store API responses for future actions
    private val apiResponses: MutableMap<String, Any> = mutableMapOf()

    // ✅ Transaction context with all API endpoints
    private val context: TransactionContext = TransactionType.MORTGAGE_CERTIFICATE.context

    // Override callbacks so ViewModel can assign them and trigger rebuilds/loading states
    override var onStepsNeedRebuild: (() -> Unit)? = null
    override var onLookupStarted: ((lookupKey: String) -> Unit)? = null
    override var onLookupCompleted: ((lookupKey: String, data: List<String>, success: Boolean) -> Unit)? = null

    /**
     * ✅ Override setHasAcceptanceFromApi to also store in formData
     * This ensures the payment success dialog can access it
     */
    override fun setHasAcceptanceFromApi(hasAcceptanceValue: Int?) {
        super.setHasAcceptanceFromApi(hasAcceptanceValue)
        // ✅ Store in formData so PaymentSuccessDialog can access it
        accumulatedFormData["hasAcceptance"] = (hasAcceptanceValue == 1).toString()
        println("🔧 MortgageCertificateStrategy: Stored hasAcceptance in formData: ${accumulatedFormData["hasAcceptance"]}")
    }

    // Helper: normalize different input date formats into ISO yyyy-MM-dd
    private fun normalizeDateToIso(input: String?): String? {
        if (input == null) return null
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        // Fast numeric fallback: match D/M/YYYY or M/D/YYYY etc (non-digit separator allowed)
        val numericDateRegex = Regex("^\\s*(\\d{1,2})\\D(\\d{1,2})\\D(\\d{2,4})\\s*$")
        val match = numericDateRegex.find(trimmed)
        if (match != null) {
            val g = match.groupValues
            val a = g[1].toIntOrNull() ?: -1
            val b = g[2].toIntOrNull() ?: -1
            var c = g[3].toIntOrNull() ?: -1
            if (c in 0..99) {
                // two-digit year -> assume 2000-based (simple heuristic)
                c += if (c >= 70) 1900 else 2000
            }
            // Try interpreting as day/month/year first (common in our locale)
            try {
                if (c > 0 && b in 1..12 && a in 1..31) {
                    val ld = LocalDate.of(c, b, a)
                    return ld.format(DateTimeFormatter.ISO_LOCAL_DATE)
                }
            } catch (_: Exception) {
                // ignore and try month/day/year
            }
            // Try month/day/year (US style)
            try {
                if (c > 0 && a in 1..12 && b in 1..31) {
                    val ld = LocalDate.of(c, a, b)
                    return ld.format(DateTimeFormatter.ISO_LOCAL_DATE)
                }
            } catch (_: Exception) {
                // fallthrough to pattern parsing
            }
        }

        val patterns = listOf(
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "yyyyMMdd",
            "d/M/yyyy",
            "d-M-yyyy",
            "d.M.yyyy",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "dd.MM.yyyy",
            "M/d/yyyy",
            "MM/dd/yyyy"
        )

        for (p in patterns) {
            try {
                val fmt = DateTimeFormatter.ofPattern(p).withResolverStyle(ResolverStyle.STRICT)
                val ld = LocalDate.parse(trimmed, fmt)
                return ld.format(DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e: DateTimeParseException) {
                // try next
            } catch (e: Exception) {
                // ignore
            }
        }

        // Last resort: try ISO parse
        return try {
            val ld = LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE)
            ld.format(DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        println("📋 ===============================================")
        println("📋 MortgageCertificate - loadDynamicOptions() CALLED")
        println("📋 ===============================================")

        // ✅ Get civilId from token
        val ownerCivilId = UserHelper.getOwnerCivilId(appContext)
        println("🔑 Owner CivilId from token: $ownerCivilId")

        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()

        // ✅ Handle null civilId - return empty list if no token
        val commercialRegistrations = if (ownerCivilId != null) {
            lookupRepository.getCommercialRegistrations(ownerCivilId).getOrNull() ?: emptyList()
        } else {
            emptyList()
        }

        println("🏦 MortgageCertificate - Fetching banks from API...")
        val banksList = lookupRepository.getBanks().getOrElse { error ->
            println("❌ ERROR fetching banks: ${error.message}")
            error.printStackTrace()
            emptyList()
        }

        println("💰 MortgageCertificate - Fetching mortgage reasons from API...")
        val mortgageReasonsList = lookupRepository.getMortgageReasons().getOrElse { error ->
            println("❌ ERROR fetching mortgage reasons: ${error.message}")
            error.printStackTrace()
            emptyList()
        }

        println("📄 MortgageCertificate - Fetching required documents from API...")
        val requestTypeId = TransactionType.MORTGAGE_CERTIFICATE.toRequestTypeId()
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
        mortgageReasons = mortgageReasonsList
        banks = banksList
        requiredDocuments = requiredDocumentsList // ✅ Store documents
        commercialOptions = commercialRegistrations
        mortgageReasons = mortgageReasonsList
        banks = banksList

        println("🚢 Skipping initial ship load - will load after user selects type and presses Next")
        println("🏦 STORING banks in member variable: size=${banks.size}, data=$banks")
        println("💰 STORING mortgageReasons in member variable: size=${mortgageReasons.size}, data=$mortgageReasons")
        println("📋 ===============================================")
        println("📋 loadDynamicOptions() COMPLETED")
        println("📋 ===============================================")

        return mapOf(
            "registrationPort" to ports,
            "ownerNationality" to countries,
            "ownerCountry" to countries,
            "bankCountry" to countries,
            "marineUnits" to emptyList<String>(),
            "commercialRegistration" to commercialRegistrations,
            "personType" to personTypes,
            "mortgagePurpose" to mortgageReasonsList,
            "bankName" to banksList
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

        println("🔍 Loading first page with loadShipsPage(page=0)")
        val firstPage = marineUnitRepository.loadShipsPage(
            ownerCivilId = ownerCivilId,
            commercialRegNumber = commercialRegNumber,
            requestTypeId = TransactionType.MORTGAGE_CERTIFICATE.toRequestTypeId(),
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
        println("📄 loadNextShipsPage (MortgageCert) page=$nextPage")
        val result = marineUnitRepository.loadShipsPage(
            ownerCivilId = ownerCivilId,
            commercialRegNumber = commercialReg,
            requestTypeId = TransactionType.MORTGAGE_CERTIFICATE.toRequestTypeId(),
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
        println("📦 MortgageCertificate - Updated accumulated data: $accumulatedFormData")
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
                allowMultipleSelection = mortgageRules.allowMultipleSelection(),
                showOwnedUnitsWarning = true
            )
        )

        // Step 4: Mortgage Data with Dynamic Documents
        println("🔍 DEBUG: Building mortgage data step with documents")
        println("🔍 DEBUG: Member variables - banks.size = ${banks.size}, mortgageReasons.size = ${mortgageReasons.size}")

        // ✅ FIX: Fetch data directly from repository cache instead of relying on member variables
        val currentBanks = runBlocking {
            lookupRepository.getBanks().getOrNull() ?: emptyList()
        }
        val currentMortgageReasons = runBlocking {
            lookupRepository.getMortgageReasons().getOrNull() ?: emptyList()
        }

        println("🔍 DEBUG: From repository cache - banks.size = ${currentBanks.size}, banks = $currentBanks")
        println("🔍 DEBUG: From repository cache - mortgageReasons.size = ${currentMortgageReasons.size}, reasons = $currentMortgageReasons")
        println("🔍 DEBUG: requiredDocuments.size = ${requiredDocuments.size}")

        steps.add(
            SharedSteps.mortgageDataStep(
                banks = currentBanks,
                mortgagePurposes = currentMortgageReasons,
                requiredDocuments = requiredDocuments  // ✅ Pass documents to be rendered in same step
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
     * Called when a step is opened - loads only the required lookups for that step
     * ✅ NEW: Loads lookups in PARALLEL with per-field loading indicators
     * ✅ ALSO: Triggers payment API call when payment step is opened
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
//                    loadLookup(lookupKey)
                }
            }
        }

        println("✅ Finished loading all lookups for step $stepIndex")

        // ✅ Rebuild steps after all lookups complete
        onStepsNeedRebuild?.invoke()
    }


    suspend fun validateMarineUnitSelection(unitId: String, userId: String): ValidationResult {
        val unit = marineUnits.find { it.id.toString() == unitId }
            ?: return ValidationResult.Error("الوحدة البحرية غير موجودة")

        println("=".repeat(80))
        println("🚢 validateMarineUnitSelection: Validating ship selection (no API call here)")
        println("   Unit ID: $unitId")
        println("   Ship Name: ${unit.shipName}")
        println("   Note: proceed-request API will be called when user clicks Next")
        println("=".repeat(80))

        // ✅ Just do the business rules validation - API call happens in processStepData
        val (validationResult, navigationAction) = validateMarineUnitUseCase.executeAndGetAction(
            unit = unit,
            userId = userId,
            rules = mortgageRules
        )

        return ValidationResult.Success(validationResult, navigationAction)
    }

    /**
     * SIMULATION: Simulates calling "استدعاء بيانات السفينة ومراجعة سجل الالتزام"
     * This represents the API that retrieves full marine unit data and checks compliance record
     */
    private fun simulateComplianceRecordCheck(
        unit: MarineUnit,
        userId: String
    ): ComplianceCheckResult {
        // SIMULATION SCENARIOS - Using EXACT maritime IDs to avoid false positives:
        // 1. Maritime ID "470123456" (first unit) - simulate violations found
        // 2. Maritime ID "OMN000123" - simulate debts found
        // 3. Maritime ID "OMN000999" - simulate detention found
        // 4. All others - proceed normally (NO ISSUES)

        val issues = mutableListOf<com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue>()

        // Scenario 1: Violations - ONLY for exact maritime ID "470123456"
        if (unit.maritimeId == "470123456") {
            issues.add(
                com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue(
                    category = "المخالفات",
                    title = "وجود مخالفات نشطة",
                    description = "تم رصد 3 مخالفات نشطة على هذه الوحدة البحرية تمنع استكمال المعاملة",
                    severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING,
                    details = mapOf(
                        "عدد المخالفات" to "3",
                        "تاريخ آخر مخالفة" to "2024-10-15",
                        "نوع المخالفة" to "مخالفة سلامة بحرية"
                    )
                )
            )
        }

        // Scenario 2: Debts - for maritime IDs containing "OMN000123"
        if (unit.maritimeId == "OMN000123") {
            issues.add(
                com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue(
                    category = "الديون والمستحقات",
                    title = "وجود ديون مستحقة",
                    description = "يوجد مبلغ مستحق غير مسدد يجب تسديده قبل المتابعة",
                    severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING,
                    details = mapOf(
                        "المبلغ المستحق" to "2,500 ريال عماني",
                        "نوع المستحق" to "رسوم تجديد سنوية",
                        "تاريخ الاستحقاق" to "2024-09-01"
                    )
                )
            )
        }

        // Scenario 3: Detention - for maritime ID "OMN000999"
//        if (unit.maritimeId == "OMN000999") {
//            issues.add(
//                com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue(
//                    category = "الاحتجازات",
//                    title = "الوحدة محتجزة",
//                    description = "الوحدة البحرية محتجزة حالياً ولا يمكن استغلالها",
//                    severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING,
//                    details = mapOf(
//                        "سبب الاحتجاز" to "مخالفة أمنية",
//                        "تاريخ الاحتجاز" to "2024-11-01",
//                        "الجهة المحتجزة" to "خفر السواحل"
//                    )
//                )
//            )
//        }

        // Build rejection reason
        val rejectionReason = if (issues.isNotEmpty()) {
            buildString {
                append("تم رفض طلبكم بسبب: ")
                issues.forEach { issue ->
                    append("\n• ${issue.title}")
                }
                append("\n\nيرجى حل هذه المشاكل أولاً ثم المحاولة مرة أخرى.")
            }
        } else {
            ""
        }

        return ComplianceCheckResult(
            issues = issues,
            rejectionReason = rejectionReason,
            validationResult = if (issues.isEmpty()) {
                com.informatique.mtcit.business.transactions.marineunit.MarineUnitValidationResult.Eligible(
                    unit = unit,
                    additionalData = emptyMap()
                )
            } else {
                com.informatique.mtcit.business.transactions.marineunit.MarineUnitValidationResult.Ineligible.CustomError(
                    unit = unit,
                    reason = rejectionReason,
                    suggestion = "يرجى حل المشاكل المذكورة أعلاه قبل المتابعة"
                )
            }
        )
    }

    /**
     * Result of compliance record check
     */
    private data class ComplianceCheckResult(
        val issues: List<com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue>,
        val rejectionReason: String,
        val validationResult: com.informatique.mtcit.business.transactions.marineunit.MarineUnitValidationResult
    ) {
        fun hasBlockingIssues(): Boolean {
            return issues.any { it.severity == com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING }
        }
    }

    override fun validateStep(step: Int, data: Map<String, Any>): Pair<Boolean, Map<String, String>> {
        val stepData = getSteps().getOrNull(step) ?: return Pair(false, emptyMap())
        val formData = data.mapValues { it.value.toString() }
        return validationUseCase.validateStep(stepData, formData)
    }

    override suspend fun processStepData(step: Int, data: Map<String, String>): Int {
        // ✅ Accumulate form data for dynamic step logic
        accumulatedFormData.putAll(data)
        println("📦 MortgageCertificate - processStepData called for step $step")
        println("📦 Current step data: $data")
        println("📦 Accumulated data: $accumulatedFormData")

        // Clear previous error
        lastApiError = null

        // Check if we just completed a step
        val currentSteps = getSteps()
        val currentStepData = currentSteps.getOrNull(step)
        val stepType = currentStepData?.stepType


        println("🔍 Current step titleRes: ${currentStepData?.titleRes}")
        println("🔍 Current step stepType: ${currentStepData?.stepType}")

        // ✅ NEW: Check if we just completed the Marine Unit Selection step (owned_ships)
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
                        createdMortgageRequestId = result.requestId
                        // Persist the request id into accumulatedFormData so getSteps can detect payment step
                        accumulatedFormData["requestId"] = result.requestId.toString()
                        accumulatedFormData["mortgageRequestId"] = result.requestId.toString()
                        apiResponses["proceedRequest"] = result.response
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
                        println("💾 STORED createdMortgageRequestId = $createdMortgageRequestId")
                    }
                    is com.informatique.mtcit.business.transactions.shared.ShipSelectionResult.Error -> {
                        println("❌ Ship selection failed: ${result.message}")
                        lastApiError = result.message
                        // ✅ Throw exception to trigger error banner display
                        throw ApiException(500, result.message)
                    }
                }
            } catch (e: ApiException) {
                println("❌ ApiException in ship selection: ${e.message}")
                lastApiError = e.message ?: "Unknown error"
                throw e // Re-throw to show error banner
            } catch (e: Exception) {
                println("❌ Exception in ship selection: ${e.message}")
                val errorMsg = com.informatique.mtcit.common.ErrorMessageExtractor.extract(e.message)
                lastApiError = errorMsg
                throw ApiException(500, errorMsg)
            }
        }

        // The mortgage data step has titleRes = R.string.mortgage_data
        if (currentStepData?.titleRes == R.string.mortgage_data) {
            println("🏦 ✅ Mortgage Data step completed - calling API to create mortgage request...")

            // Call the API in a blocking way (will be handled in coroutine context)
            var apiCallSucceeded = false
            try {
                val result = createMortgageRequest(accumulatedFormData)
                result.fold(
                    onSuccess = { response ->
                        println("✅ Mortgage request created successfully!")
                        println("   Mortgage ID: ${response.data.id}")
                        println("   Message: ${response.message}")

                        // ✅ CRITICAL: Store the mortgage request ID in the member variable
                        createdMortgageRequestId = response.data.id
                        println("💾 STORED createdMortgageRequestId = $createdMortgageRequestId")

                        // Store the mortgage request ID for later use
                        accumulatedFormData["mortgageRequestId"] = response.data.id.toString()
                        // Also store a generic requestId key so payment step detection is consistent
                        accumulatedFormData["requestId"] = response.data.id.toString()
                        lastApiError = null // Clear any previous error
                        apiCallSucceeded = true
                    },
                    onFailure = { error ->
                    println("❌ Failed to add crew: ${error.message}")
                    // Store API error for UI / debugging
                    val msg = when (error) {
                        is com.informatique.mtcit.common.ApiException -> error.message ?: "فشل في إضافة الطاقم"
                        else -> error.message ?: "فشل في إضافة الطاقم"
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
                println("❌ Exception while creating mortgage request: ${e.message}")
                e.printStackTrace()

                // Store error for Toast display
                lastApiError = e.message ?: "حدث خطأ غير متوقع"
                apiCallSucceeded = false
            }

            // Return -1 to prevent navigation if API call failed
            if (!apiCallSucceeded) {
                println("⚠️ API call failed - returning -1 to prevent navigation")
                return -1
            }
        }

        // ✅ HANDLE REVIEW STEP - Use ReviewManager
        if (currentStepData?.stepType == StepType.REVIEW) {
            println("📋 Handling Review Step using ReviewManager for Mortgage Certificate")

            // Determine request id from any available source
            val requestIdInt = createdMortgageRequestId
                ?: accumulatedFormData["requestId"]?.toIntOrNull()
                ?: accumulatedFormData["mortgageRequestId"]?.toIntOrNull()
            if (requestIdInt == null) {
                println("❌ No mortgageRequestId available for review step")
                lastApiError = "لم يتم العثور على رقم طلب الرهن"
                return -1
            }

            try {
                // ✅ Get endpoint and context from transactionContext
                val transactionContext = TransactionType.MORTGAGE_CERTIFICATE.context
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
                            println("🎉 NEW mortgage request submitted with hasAcceptance=true - showing success dialog and stopping")
                            println("   User must continue from profile screen")

                            // Set success flags for ViewModel to show dialog
                            accumulatedFormData["requestSubmitted"] = "true"
                            accumulatedFormData["requestNumber"] = requestNumber
                            accumulatedFormData["successMessage"] = reviewResult.message

                            // Return -2 to indicate: success but show dialog and stop
                            return -2
                        } else if (isNewRequest && !strategyHasAcceptance) {
                            println("✅ NEW mortgage request submitted with hasAcceptance=false - continuing to next steps")
                            println("   Transaction will continue to payment/next steps")
                            // Continue normally - don't return, let the flow proceed
                        } else {
                            println("✅ Resumed mortgage request - using existing resume logic")
                        }

                        // ✅ MORTGAGE CERTIFICATE: Different response handling for resumed requests
                        // For mortgage, we check for bankVerification, approvalStatus, etc.

                        // Check additionalData for mortgage-specific fields
                        val bankVerification = reviewResult.additionalData?.get("bankVerification") as? String
                        val approvalStatus = reviewResult.additionalData?.get("approvalStatus") as? String
                        val documentReview = reviewResult.additionalData?.get("documentReview") as? String

                        if (bankVerification == "pending") {
                            println("🏦 Bank verification pending")
                            accumulatedFormData["showBankVerificationDialog"] = "true"
                            accumulatedFormData["verificationMessage"] = reviewResult.message
                            return step // Stay on current step
                        }

                        if (approvalStatus == "pending") {
                            println("⏳ Approval pending")
                            accumulatedFormData["showApprovalPendingDialog"] = "true"
                            accumulatedFormData["approvalMessage"] = reviewResult.message
                            return step // Stay on current step
                        }

                        if (documentReview == "required") {
                            println("📄 Document review required")
                            accumulatedFormData["showDocumentReviewDialog"] = "true"
                            accumulatedFormData["reviewMessage"] = reviewResult.message
                            return step // Stay on current step
                        }

                        // ✅ Also support needInspection (common field)
                        if (reviewResult.needInspection) {
                            println("🔍 Inspection required - showing dialog")
                            accumulatedFormData["showInspectionDialog"] = "true"
                            accumulatedFormData["inspectionMessage"] = reviewResult.message
                            return step // Stay on current step
                        }

                        // Proceed - request submitted successfully
                        println("✅ No blocking conditions - mortgage request submitted successfully")
                        // Persist request id into formData to ensure payment step is shown
                        accumulatedFormData["requestId"] = requestIdInt.toString()
                        accumulatedFormData["mortgageRequestId"] = requestIdInt.toString()
                    }
                    is com.informatique.mtcit.business.transactions.shared.ReviewResult.Error -> {
                        println("❌ Review step failed: ${reviewResult.message}")
                        lastApiError = reviewResult.message
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
                lastApiError = "حدث خطأ أثناء إرسال الطلب: ${e.message}"
                return -1
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
                is com.informatique.mtcit.business.transactions.shared.StepProcessResult.Success -> {
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

    /**
     * Create a mortgage request from the accumulated form data
     */
    private suspend fun createMortgageRequest(formData: Map<String, String>): Result<com.informatique.mtcit.data.model.CreateMortgageResponse> {
        // Extract data from form
        // The marine unit selection field ID is "selectedMarineUnits"
        val selectedUnitsJson = formData["selectedMarineUnits"] // This is a JSON array
        val bankName = formData["bankName"]
        val mortgagePurpose = formData["mortgagePurpose"]
        val mortgageContractNumber = formData["mortgageContractNumber"]
        val mortgageValue = formData["mortgageValue"]
        val mortgageStartDate = formData["mortgageStartDate"]

        println("📋 Creating mortgage request with:")
        println("   Selected Units JSON: $selectedUnitsJson")
        println("   Bank: $bankName")
        println("   Purpose: $mortgagePurpose")
        println("   Contract Number: $mortgageContractNumber")
        println("   Value: $mortgageValue")
        println("   Start Date: $mortgageStartDate")

        // Validate required fields
        if (selectedUnitsJson.isNullOrBlank() || selectedUnitsJson == "[]") {
            return Result.failure(Exception("Marine unit not selected"))
        }
        if (bankName.isNullOrBlank()) {
            return Result.failure(Exception("Bank not selected"))
        }
        if (mortgagePurpose.isNullOrBlank()) {
            return Result.failure(Exception("Mortgage purpose not selected"))
        }
        if (mortgageContractNumber.isNullOrBlank()) {
            return Result.failure(Exception("Contract number is required"))
        }
        if (mortgageValue.isNullOrBlank()) {
            return Result.failure(Exception("Mortgage mortgageValue is required"))
        }
        if (mortgageStartDate.isNullOrBlank()) {
            return Result.failure(Exception("Start date is required"))
        }

        // Parse the selected units JSON (it's an array like ["321"] - these are shipInfoIds)
        val shipId = try {
            // Remove brackets and quotes, split by comma, take first
            val cleanJson = selectedUnitsJson.trim().removeSurrounding("[", "]")
            val shipIds = cleanJson.split(",").map { it.trim().removeSurrounding("\"") }
            val firstShipId = shipIds.firstOrNull()

            if (firstShipId.isNullOrBlank()) {
                println("❌ Failed to parse ship ID from: $selectedUnitsJson")
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

        // Get IDs from the lookup repository
        val bankIdValue: Int? = lookupRepository.getBankId(bankName)
        val mortgageReasonIdValue: Int? = lookupRepository.getMortgageReasonId(mortgagePurpose)

        println("🔍 ID Lookup Results:")
        println("   Bank Name: '$bankName' → Bank ID: '$bankIdValue' (type: ${bankIdValue?.javaClass?.simpleName})")
        println("   Mortgage Purpose: '$mortgagePurpose' → Reason ID: $mortgageReasonIdValue")

        if (bankIdValue == null) {
            println("❌ Bank ID is null - available banks in cache:")
            // This will help debug what banks are available
            return Result.failure(Exception("Bank ID not found for: $bankName"))
        }
        if (mortgageReasonIdValue == null) {
            return Result.failure(Exception("Mortgage reason ID not found for: $mortgagePurpose"))
        }


        // Parse mortgage mortgageValue
        val valueDouble = mortgageValue.toDoubleOrNull() ?: run {
            return Result.failure(Exception("Invalid mortgage mortgageValue: $mortgageValue"))
        }

        // Normalize the start date to ISO yyyy-MM-dd
        val normalizedStartDate = normalizeDateToIso(mortgageStartDate)
            ?: return Result.failure(Exception("Invalid start date format: $mortgageStartDate (expected YYYY-MM-DD)"))

        // Create the request
        val request = com.informatique.mtcit.data.model.CreateMortgageRequest(
            shipInfoId = shipId,
            bankId = bankIdValue,  // Now guaranteed to be non-null Int
            mortgageReasonId = mortgageReasonIdValue,  // Now guaranteed to be non-null Int
            financingContractNumber = mortgageContractNumber,
            startDate = normalizedStartDate,
            mortgageValue = valueDouble
            // statusId is automatic = 1
        )

        println("=".repeat(80))
        println("🔍🔍🔍 DEBUG: MORTGAGE VALUE TRACKING 🔍🔍🔍")
        println("=".repeat(80))
        println("📥 INPUT from form field 'mortgageValue': '$mortgageValue' (type: ${mortgageValue.javaClass.simpleName})")
        println("🔢 PARSED to Double: $valueDouble (type: ${valueDouble.javaClass.simpleName})")
        println("📦 REQUEST OBJECT being sent:")
        println("   request.mortgageValue = $valueDouble")
        println("   request.shipId = $shipId")
        println("   request.bankId = $bankIdValue")
        println("   request.mortgageReasonId = $mortgageReasonIdValue")
        println("   request.financingContractNumber = '$mortgageContractNumber'")
        println("   request.startDate = '$normalizedStartDate'")
        println("=".repeat(80))
        println("📤 Sending mortgage request to API...")

        // ✅ NEW: Collect all uploaded documents from dynamic fields
        val uploadedDocuments = mutableListOf<OwnerFileUpload>()

        // Get all document fields (document_43, document_44, etc.)
        requiredDocuments
            .filter { it.document.isActive == 1 }
            .forEach { docItem ->
                val fieldId = "document_${docItem.document.id}"
                val documentUri = formData[fieldId]

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
                                docOwnerId = "document_${docItem.document.id}", // ✅ Use document ID
                                docId = docItem.document.id // ✅ Send the actual document ID from API
                            )

                            uploadedDocuments.add(ownerFile)
                            println("📎 Added document: ${docItem.document.nameAr} (id=${docItem.document.id}, file=${ownerFile.fileName}, mimeType=$properMimeType)")
                        }
                    } catch (e: Exception) {
                        println("⚠️ Failed to process document ${docItem.document.nameAr}: ${e.message}")
                    }
                }
            }

        println("📋 Total documents to upload: ${uploadedDocuments.size}")

        // ✅ FINAL STRATEGY: Use multipart with 'request' field (JSON as Text) + files
        println("📤 Creating mortgage request with multipart/form-data (request + files)...")
        val result = mortgageApiService.createMortgageRequestWithDocuments(request, uploadedDocuments)

        result.onSuccess { response ->
            createdMortgageRequestId = response.data.id
            println("=".repeat(80))
            println("💾 STORED MORTGAGE REQUEST ID: $createdMortgageRequestId")
            println("=".repeat(80))

            if (uploadedDocuments.isNotEmpty()) {
                println("✅ Uploaded documents:")
                uploadedDocuments.forEach { doc ->
                    println("   - ${doc.fileName} (docId=${doc.docId})")
                }
            } else {
                println("ℹ️ No documents uploaded")
            }
        }

        result .onFailure { error ->
            println("❌ Create mortgage request failed: ${error.message}")
            // Build friendly message
            val msg = when (error) {
                is com.informatique.mtcit.common.ApiException -> error.message ?: "فشل في إنشاء طلب الرهن"
                else -> com.informatique.mtcit.common.ErrorMessageExtractor.extract(error.message)
            }
            // Store for UI and debugging
            accumulatedFormData["apiError"] = msg
            lastApiError = msg

            // Re-throw as ApiException so upstream processStepData will catch and surface banner
            if (error is com.informatique.mtcit.common.ApiException) {
                throw error
            } else {
                throw com.informatique.mtcit.common.ApiException(400, msg)
            }
        }

        return result
    }

    /**
     * ⚠️ DEPRECATED: This method is no longer used in the simplified flow
     * The review submission now goes directly through submitOnReview() in ViewModel
     * Kept only for interface compliance with TransactionStrategy
     */
    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        return Result.success(true)
    }

    override fun handleFieldChange(fieldId: String, value: String, formData: Map<String, String>): Map<String, String> {
        // Handle person type change to show/hide commercial registration step
        if (fieldId == "selectionPersonType") {
            val mutableFormData = formData.toMutableMap()
            when (value) {
                "INDIVIDUAL" -> {
                    // Clear company-related fields if switching to individual
                    mutableFormData.remove("companyRegistrationNumber")
                    mutableFormData.remove("companyName")
                    mutableFormData.remove("companyType")
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

    /**
     * ✅ Get the created mortgage request ID
     * Used in review step to submit status update
     */
    fun getCreatedMortgageRequestId(): Int? {
        println("🔍 getCreatedMortgageRequestId() called")
        println("   createdMortgageRequestId = $createdMortgageRequestId")
        println("   accumulatedFormData['mortgageRequestId'] = ${accumulatedFormData["mortgageRequestId"]}")

        // ✅ Fallback: Try to get from accumulated form data if member variable is null
        if (createdMortgageRequestId == null) {
            val idFromFormData = accumulatedFormData["mortgageRequestId"]?.toIntOrNull()
            if (idFromFormData != null) {
                println("⚠️ createdMortgageRequestId was null, using value from formData: $idFromFormData")
                createdMortgageRequestId = idFromFormData
            }
        }

        return createdMortgageRequestId
    }

    // ✅ Implement TransactionStrategy interface methods for dynamic status update

    override fun getStatusUpdateEndpoint(requestId: Int): String {
        return context.buildUpdateStatusUrl(requestId)
    }

    override fun getSendRequestEndpoint(requestId: Int): String {
        return context.buildSendRequestUrl(requestId)
    }

    override fun getCreatedRequestId(): Int? {
        return getCreatedMortgageRequestId()
    }

    override fun getTransactionTypeName(): String {
        return "Mortgage"
    }

    /**
     * ✅ Get the transaction context with all API endpoints
     */
    override fun getContext(): TransactionContext {
        return TransactionType.MORTGAGE_CERTIFICATE.context
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

    // ================================================================================
    // 🎯 DRAFT TRACKING: Extract completed steps from API response
    // ================================================================================
    override fun extractCompletedStepsFromApiResponse(response: Any): Set<StepType> {
        // TODO: Parse the mortgage certificate API response
        val completedSteps = mutableSetOf<StepType>()

        println("⚠️ MortgageCertificateStrategy: extractCompletedStepsFromApiResponse not yet implemented")
        println("   Response type: ${response::class.simpleName}")

        return completedSteps
    }
}
