package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.R
import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.SharedSteps
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
import com.informatique.mtcit.business.transactions.marineunit.rules.MortgageCertificateRules
import com.informatique.mtcit.business.transactions.marineunit.usecases.ValidateMarineUnitUseCase
import com.informatique.mtcit.business.transactions.marineunit.usecases.GetEligibleMarineUnitsUseCase
import com.informatique.mtcit.data.repository.MarineUnitRepository
import android.content.Context
import com.informatique.mtcit.data.helpers.FileUploadHelper
import com.informatique.mtcit.data.model.OwnerFileUpload
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val marineUnitRepository: MarineUnitRepository,
    private val mortgageApiService: com.informatique.mtcit.data.api.MortgageApiService,
    @ApplicationContext private val appContext: Context
) : TransactionStrategy {

    private var portOptions: List<String> = emptyList()
    private var countryOptions: List<String> = emptyList()
    private var personTypeOptions: List<PersonType> = emptyList()
    private var commercialOptions: List<SelectableItem> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()
    private var mortgageReasons: List<String> = emptyList()
    private var banks: List<String> = emptyList()
    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf()

    // Store API error to prevent navigation and show error dialog
    private var lastApiError: String? = null

    // ✅ NEW: Store created mortgage request ID for status update
    private var createdMortgageRequestId: Int? = null

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

        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()
        val commercialRegistrations = lookupRepository.getCommercialRegistrations("12345678901234").getOrNull() ?: emptyList()

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

        portOptions = ports
        countryOptions = countries
        personTypeOptions = personTypes
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

        // ✅ UPDATED: For companies, use commercialReg (crNumber) from selectionData
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
            requestTypeId = TransactionType.MORTGAGE_CERTIFICATE.toRequestTypeId() // ✅ Mortgage Certificate ID
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
        println("📦 MortgageCertificate - Updated accumulated data: $accumulatedFormData")
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

        // Step 4: Mortgage Data
        println("🔍 DEBUG: Building mortgage data step")
        println("🔍 DEBUG: Member variables - banks.size = ${banks.size}, mortgageReasons.size = ${mortgageReasons.size}")

        // ✅ FIX: Fetch data directly from repository cache instead of relying on member variables
        // This ensures data is available even if Strategy instance was recreated
        // Using runBlocking is safe here because repository uses in-memory cache
        val currentBanks = runBlocking {
            lookupRepository.getBanks().getOrNull() ?: emptyList()
        }
        val currentMortgageReasons = runBlocking {
            lookupRepository.getMortgageReasons().getOrNull() ?: emptyList()
        }

        println("🔍 DEBUG: From repository cache - banks.size = ${currentBanks.size}, banks = $currentBanks")
        println("🔍 DEBUG: From repository cache - mortgageReasons.size = ${currentMortgageReasons.size}, reasons = $currentMortgageReasons")

        steps.add(
            SharedSteps.mortgageDataStep(
                banks = currentBanks,
                mortgagePurposes = currentMortgageReasons
            )
        )

        // Step 5: Review
        steps.add(SharedSteps.reviewStep())

        return steps
    }

    suspend fun validateMarineUnitSelection(unitId: String, userId: String): ValidationResult {
        val unit = marineUnits.find { it.id.toString() == unitId }
            ?: return ValidationResult.Error("الوحدة البحرية غير موجودة")

        // SIMULATION: استدعاء بيانات السفينة ومراجعة سجل الالتزام
        // Simulate API call to retrieve ship data and review compliance record
        val complianceCheckResult = simulateComplianceRecordCheck(unit, userId)

        // If compliance check finds blocking issues, return ShowComplianceDetailScreen
        if (complianceCheckResult.hasBlockingIssues()) {
            return ValidationResult.Success(
                validationResult = complianceCheckResult.validationResult,
                navigationAction = com.informatique.mtcit.business.transactions.marineunit.MarineUnitNavigationAction.ShowComplianceDetailScreen(
                    marineUnit = unit,
                    complianceIssues = complianceCheckResult.issues,
                    rejectionReason = complianceCheckResult.rejectionReason,
                    rejectionTitle = "تم رفض الطلب - مشاكل في سجل الالتزام"
                )
            )
        }

        // Otherwise, proceed with normal validation
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

        // Check if we just completed the Mortgage Data step
        val currentSteps = getSteps()
        val currentStepData = currentSteps.getOrNull(step)

        println("🔍 Current step titleRes: ${currentStepData?.titleRes}")
        println("🔍 R.string.mortgage_data mortgageValue: ${R.string.mortgage_data}")

        // The mortgage data step has titleRes = R.string.mortgage_data
        if (currentStepData?.titleRes == R.string.mortgage_data) {
            println("🏦 ✅ Mortgage Data step completed - calling API to create mortgage request...")

            // Call the API in a blocking way (will be handled in coroutine context)
            var apiCallSucceeded = false
            runBlocking {
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
                            lastApiError = null // Clear any previous error
                            apiCallSucceeded = true
                        },
                        onFailure = { error ->
                            println("❌ Failed to create mortgage request: ${error.message}")
                            error.printStackTrace()

                            // Store error for Toast display
                            lastApiError = error.message ?: "حدث خطأ أثناء إنشاء طلب الرهن"
                            apiCallSucceeded = false
                        }
                    )
                } catch (e: Exception) {
                    println("❌ Exception while creating mortgage request: ${e.message}")
                    e.printStackTrace()

                    // Store error for Toast display
                    lastApiError = e.message ?: "حدث خطأ غير متوقع"
                    apiCallSucceeded = false
                }
            }

            // Return -1 to prevent navigation if API call failed
            if (!apiCallSucceeded) {
                println("⚠️ API call failed - returning -1 to prevent navigation")
                return -1
            }
        } else {
            println("ℹ️ This is not the mortgage data step, skipping API call")
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

        // Parse the selected units JSON (it's an array like ["987654321"] - these are maritimeIds/MMSI numbers)
        // We need to find the actual ship ID from the MarineUnit objects
        val shipId = try {
            // Remove brackets and quotes, split by comma, take first
            val cleanJson = selectedUnitsJson.trim().removeSurrounding("[", "]")
            val maritimeIds = cleanJson.split(",").map { it.trim().removeSurrounding("\"") }
            val firstMaritimeId = maritimeIds.firstOrNull()

            if (firstMaritimeId.isNullOrBlank()) {
                println("❌ Failed to parse maritime ID from: $selectedUnitsJson")
                return Result.failure(Exception("Invalid marine unit selection format"))
            }

            println("📍 Extracted maritime ID (MMSI): $firstMaritimeId")

            // Find the MarineUnit object that matches this maritimeId
            val selectedUnit = marineUnits.firstOrNull { it.maritimeId == firstMaritimeId }
            if (selectedUnit == null) {
                println("❌ Could not find MarineUnit with maritimeId: $firstMaritimeId")
                println("   Available units: ${marineUnits.map { "maritimeId=${it.maritimeId}, id=${it.id}" }}")
                return Result.failure(Exception("Selected marine unit not found in available units"))
            }

            // Convert the actual ship ID (from database) to Int
            val actualShipId = selectedUnit.id.toIntOrNull()
            if (actualShipId == null) {
                println("❌ Ship ID is not a valid integer: ${selectedUnit.id}")
                return Result.failure(Exception("Invalid ship ID format"))
            }

            println("✅ Found matching MarineUnit:")
            println("   Maritime ID (MMSI): ${selectedUnit.maritimeId}")
            println("   Actual Ship ID (database): $actualShipId")
            println("   Ship Name: ${selectedUnit.shipName}")
            println("   IMO Number: ${selectedUnit.imoNumber}")

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

        // Check if user attached a file in the mortgageApplication field
        val mortgageApplicationUri = formData["mortgageApplication"]
        if (!mortgageApplicationUri.isNullOrBlank() && mortgageApplicationUri.startsWith("content://")) {
            try {
                val uri = android.net.Uri.parse(mortgageApplicationUri)
                val fileUpload = FileUploadHelper.uriToFileUpload(appContext, uri)
                if (fileUpload != null) {
                    val ownerFile = OwnerFileUpload(
                        fileName = fileUpload.fileName,
                        fileUri = fileUpload.fileUri,
                        fileBytes = fileUpload.fileBytes,
                        mimeType = fileUpload.mimeType ?: "application/octet-stream",
                        docOwnerId = "mortgageApplication",
                        docId = 1
                    )

                    println("📎 Found mortgage application file: ${ownerFile.fileName} - uploading multipart")
                    // Try regular multipart first
                    val result = mortgageApiService.createMortgageRequestWithDocuments(request, listOf(ownerFile))

                    // Debug: log the returned value
                    result.onSuccess { response ->
                        // ✅ Store the created request ID
                        createdMortgageRequestId = response.data.id
                        println("=".repeat(80))
                        println("💾 STORED MORTGAGE REQUEST ID (MULTIPART): $createdMortgageRequestId")
                        println("=".repeat(80))

                        println("=".repeat(80))
                        println("🔍🔍🔍 API RESPONSE VALUE CHECK 🔍🔍🔍")
                        println("=".repeat(80))
                        println("📤 SENT to API: mortgageValue = $valueDouble")
                        println("📥 RECEIVED from API: response.data.mortgageValue = ${response.data.mortgageValue}")
                        println("⚠️ COMPARISON: Sent=$valueDouble, Received=${response.data.mortgageValue}")
                        if (response.data.mortgageValue != valueDouble) {
                            println("❌❌❌ VALUE MISMATCH! Backend returned different value!")
                            println("   This means: CLIENT sent correct value but SERVER stored/returned 0.0")
                            println("   Action needed: Backend team must check mapping/DB defaults")
                        } else {
                            println("✅✅✅ VALUE MATCH! Backend correctly stored the value")
                        }
                        println("=".repeat(80))
                    }
                    result.onFailure { error ->
                        println("❌ createMortgageRequestWithDocuments failed: ${error.message}")
                    }

                    if (result.isSuccess) return result

                    // If failed, inspect message and retry with flat multipart if likely server expects flat form fields
                    val errMsg = result.exceptionOrNull()?.message ?: ""
                    println("⚠️ createMortgageRequestWithDocuments failed: $errMsg")
                    val shouldRetryFlat = listOf("mortgage_value", "MORTGAGE_VALUE", "Content-Type 'application/json' is not supported", "cannot insert NULL into", "ORA-01400").any { errMsg.contains(it, ignoreCase = true) }
                    if (shouldRetryFlat) {
                        println("🔁 Retrying with flat multipart fields (createMortgageRequestWithDocumentsFlat)")
                        val retryResult = mortgageApiService.createMortgageRequestWithDocumentsFlat(request, listOf(ownerFile))

                        // ✅ Store the created request ID from retry
                        retryResult.onSuccess { response ->
                            createdMortgageRequestId = response.data.id
                            println("💾 STORED MORTGAGE REQUEST ID (FLAT MULTIPART RETRY): $createdMortgageRequestId")
                        }

                        return retryResult
                    }

                    return result
                } else {
                    println("⚠️ Could not convert mortgageApplication URI to file (uri=$mortgageApplicationUri)")
                }
            } catch (e: Exception) {
                println("❌ Exception converting mortgageApplication URI to file: ${e.message}")
                e.printStackTrace()
            }
        }

        // No file attached or failed to convert -> fallback to JSON POST
        val result = mortgageApiService.createMortgageRequest(request)

        // Debug: log the returned value
        result.onSuccess { response ->
            // ✅ Store the created request ID
            createdMortgageRequestId = response.data.id
            println("=".repeat(80))
            println("💾 STORED MORTGAGE REQUEST ID: $createdMortgageRequestId")
            println("=".repeat(80))

            println("=".repeat(80))
            println("🔍🔍🔍 API RESPONSE VALUE CHECK (JSON POST) 🔍🔍🔍")
            println("=".repeat(80))
            println("📤 SENT to API: mortgageValue = $valueDouble")
            println("📥 RECEIVED from API: response.data.mortgageValue = ${response.data.mortgageValue}")
            println("⚠️ COMPARISON: Sent=$valueDouble, Received=${response.data.mortgageValue}")
            if (response.data.mortgageValue != valueDouble) {
                println("❌❌❌ VALUE MISMATCH! Backend returned different value!")
                println("   This means: CLIENT sent correct value but SERVER stored/returned 0.0")
                println("   Action needed: Backend team must check mapping/DB defaults")
            } else {
                println("✅✅✅ VALUE MATCH! Backend correctly stored the value")
            }
            println("=".repeat(80))
        }
        result.onFailure { error ->
            println("❌ createMortgageRequest (JSON) failed: ${error.message}")
        }

        return result
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        return repository.submitRegistration(data)
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
        return "api/v1/mortgage-request/$requestId/update-status"
    }

    override fun getCreatedRequestId(): Int? {
        return getCreatedMortgageRequestId()
    }

    override fun getTransactionTypeName(): String {
        return "Mortgage"
    }
}
