package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.business.transactions.managers.NavigationLicenseManager
import com.informatique.mtcit.business.transactions.shared.StepType
import com.informatique.mtcit.business.transactions.shared.PaymentManager
import com.informatique.mtcit.data.model.NavigationArea
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.data.repository.MarineUnitRepository
import com.informatique.mtcit.ui.components.PersonType
import com.informatique.mtcit.ui.components.SelectableItem
import com.informatique.mtcit.ui.repo.CompanyRepo
import com.informatique.mtcit.ui.viewmodels.StepData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Strategy for Issue Navigation Permit
 * Uses NavigationLicenseManager for all navigation license operations
 */
class IssueNavigationPermitStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val companyRepository: CompanyRepo,
    private val validationUseCase: FormValidationUseCase,
    private val marineUnitRepository: MarineUnitRepository,
    private val lookupRepository: LookupRepository,
    private val navigationLicenseManager: NavigationLicenseManager,
    private val paymentManager: PaymentManager
 ) : TransactionStrategy {

    private var countryOptions: List<String> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()

    private var commercialOptions: List<SelectableItem> = emptyList()

    private var typeOptions: List<PersonType> = emptyList()
    private var sailingRegionsOptions: List<NavigationArea> = emptyList()
    private var crewJobTitles: List<String> = emptyList()

    // Cache for accumulated form data (used to decide steps like other strategies)
    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf()

    // ✅ Navigation license specific state
    private var navigationRequestId: Long? = null // Store created request ID

    // Allow ViewModel to set a callback when steps need to be rebuilt (same pattern as other strategies)
    override var onStepsNeedRebuild: (() -> Unit)? = null

    /**
     * Load ships for the selected person type / commercial registration.
     * This mirrors the behavior in Temporary/Permanent strategies so the UI can show owned ships.
     */
    override suspend fun loadShipsForSelectedType(formData: Map<String, String>): List<MarineUnit> {
        val personType = formData["selectionPersonType"]
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
            requestTypeId = TransactionType.ISSUE_NAVIGATION_PERMIT.toRequestTypeId() // ✅ Issue Navigation Permit ID
        )

        println("✅ IssueNavigationPermit - Loaded ${marineUnits.size} ships")
        marineUnits.forEach { println("   - ${it.shipName} (ID: ${it.id})") }

        return marineUnits
    }

    override suspend fun clearLoadedShips() {
        marineUnits = emptyList()
    }

    override fun updateAccumulatedData(data: Map<String, String>) {
        accumulatedFormData.putAll(data)
    }

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val commercialRegistrations = lookupRepository.getCommercialRegistrations("12345678901234").getOrNull() ?: emptyList()
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()

        countryOptions = countries
        commercialOptions = commercialRegistrations
        typeOptions = personTypes


        return mapOf(
            "marineUnits" to emptyList<MarineUnit>(), // ✅ Empty initially
            "registrationCountry" to countries,
            "commercialRegistration" to commercialRegistrations,
            "personType" to personTypes
        )
    }

    // Load lookups when a step is opened (lazy loading)
    override suspend fun onStepOpened(stepIndex: Int) {
        val step = getSteps().getOrNull(stepIndex) ?: return
        if (step.requiredLookups.isEmpty()) return

        step.requiredLookups.forEach { lookupKey ->
            when (lookupKey) {
                "sailingRegions" -> {
                    if (sailingRegionsOptions.isEmpty()) {
                        val areas = lookupRepository.getNavigationAreas().getOrNull() ?: emptyList()
                        sailingRegionsOptions = areas
                    }
                }
                "crewJobTitles" -> {
                    if (crewJobTitles.isEmpty()) {
                        val jobs = lookupRepository.getCrewJobTitles().getOrNull() ?: emptyList()
                        crewJobTitles = jobs
                    }
                }
                 // add other lookups if needed
            }
        }

        // Notify UI to refresh steps so dropdown picks up new data
        onStepsNeedRebuild?.invoke()
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

        steps.add(
            SharedSteps.marineUnitSelectionStep(
                units = marineUnits,
                showAddNewButton = false
            )
        )
        steps.add(SharedSteps.sailingRegionsStep(
            sailingRegions = sailingRegionsOptions.map { it.nameAr } // ✅ Pass names to UI
        ))
        steps.add( SharedSteps.sailorInfoStep(
            jobs = crewJobTitles
        ))

        // Review Step (shows all collected data)
        steps.add(SharedSteps.reviewStep())
        steps.add(SharedSteps.paymentDetailsStep()) // Add payment step here

        println("📋 Total steps count: ${steps.size}")
        return steps
    }

    override fun validateStep(step: Int, data: Map<String, Any>): Pair<Boolean, Map<String, String>> {
        val stepData = getSteps().getOrNull(step) ?: return Pair(false, emptyMap())
        val formData = data.mapValues { it.value.toString() }
        return validationUseCase.validateStep(stepData, formData)
    }

    override suspend fun processStepData(step: Int, data: Map<String, String>): Int {
        // Update accumulated data first
        accumulatedFormData.putAll(data)

        val stepData = getSteps().getOrNull(step)

        // ✅ Use stepType instead of checking field IDs
        when (stepData?.stepType) {
            StepType.NAVIGATION_AREAS -> {
                val hasError = handleNavigationAreasSubmission(data)
                if (hasError) return -1 // ✅ Block navigation if error occurred
            }
            StepType.CREW_MANAGEMENT -> {
                val hasError = handleCrewSubmission(data)
                if (hasError) return -1 // ✅ Block navigation if error occurred
            }
            else -> {}
        }

        return step
    }

    /**
     * Handle navigation areas submission
     * @return true if error occurred, false if successful
     */
    private suspend fun handleNavigationAreasSubmission(data: Map<String, String>): Boolean {
        // ✅ Get selected names from form data - handle JSON array format
        val sailingRegionsString = data["sailingRegions"] ?: ""

        // Parse JSON array: ["المنطقة 1","المنطقة 2","المنطقة 7"]
        val selectedNames = if (sailingRegionsString.startsWith("[") && sailingRegionsString.endsWith("]")) {
            // Remove brackets and split by comma, then clean quotes and trim
            sailingRegionsString
                .substring(1, sailingRegionsString.length - 1) // Remove [ and ]
                .split(",")
                .map { it.trim().removeSurrounding("\"") } // Remove quotes and trim
                .filter { it.isNotEmpty() }
        } else {
            emptyList()
        }

        println("🔍 Raw sailingRegions data: $sailingRegionsString")
        println("🔍 Parsed selected names: $selectedNames")
        println("🔍 Available regions in cache: ${sailingRegionsOptions.map { "${it.id}:${it.nameAr}" }}")

        // ✅ Map names to IDs
        val selectedAreaIds = sailingRegionsOptions
            .filter { area -> selectedNames.contains(area.nameAr) }
            .map { it.id }

        if (selectedAreaIds.isEmpty()) {
            println("⚠️ No navigation areas selected or no matching IDs found")
            println("⚠️ Selected names: $selectedNames")
            println("⚠️ Available regions: ${sailingRegionsOptions.map { it.nameAr }}")
            return false
        }

        println("✅ Selected navigation areas: names=$selectedNames, ids=$selectedAreaIds")

        // Ensure we have a request ID (create request if needed)
        val requestId = ensureRequestCreated()

        // ✅ Check if request creation failed
        if (requestId == null) {
            println("❌ Failed to create/get navigation request - blocking navigation")
            // Error details are already stored in accumulatedFormData by ensureRequestCreated()
            return true // ✅ Return error to block navigation
        }

        // ✅ Call API and check result before continuing
        val result = navigationLicenseManager.addNavigationAreasIssue(requestId, selectedAreaIds)

        if (result.isFailure) {
            // ✅ Handle error and prevent navigation
            val error = result.exceptionOrNull()
            println("❌ Failed to add navigation areas: ${error?.message}")

            // ✅ Parse error from message format: "API Error 406: {json}"
            val errorMessage = error?.message ?: ""

            // Try to extract error code and message from "API Error XXX: {json}" format
            val errorCodeRegex = """API Error (\d+):\s*(.*)""".toRegex()
            val match = errorCodeRegex.find(errorMessage)

            if (match != null) {
                val code = match.groupValues[1] // e.g., "406"
                val jsonPart = match.groupValues[2] // e.g., {"timestamp":"...","message":"..."}

                // Try to parse JSON to extract the message
                val messageFromJson = try {
                    // Simple extraction of "message" field from JSON
                    val messageRegex = """"message"\s*:\s*"([^"]*)"""".toRegex()
                    val messageMatch = messageRegex.find(jsonPart)
                    messageMatch?.groupValues?.get(1) ?: jsonPart
                } catch (e: Exception) {
                    jsonPart
                }

                // Store in formData for UI to display
                accumulatedFormData["apiErrorCode"] = code
                accumulatedFormData["apiErrorMessage"] = messageFromJson

                println("🔍 Extracted error code: $code, message: $messageFromJson")

                // If it's not a 406 error, show generic dialog instead
                if (code != "406") {
                    accumulatedFormData["apiError"] = errorMessage
                } else {
                    // For 406 errors, also set apiError to trigger the dialog
                    accumulatedFormData["apiError"] = messageFromJson
                }
            } else {
                // Fallback: store the raw error message
                accumulatedFormData["apiError"] = errorMessage
            }

            // ✅ Return true to indicate error occurred
            return true
        }

        // ✅ Success case
        println("✅ Navigation areas added successfully")
        return false
    }

    /**
     * Handle crew submission (manual or Excel)
     * @return true if error occurred, false if successful
     */
    private suspend fun handleCrewSubmission(data: Map<String, String>): Boolean {
        val requestId = ensureRequestCreated()

        // If request creation failed, return error
        if (requestId == null) {
            println("❌ Cannot add crew - no request ID available")
            return true
        }

        // Check if user chose Excel upload
        if (navigationLicenseManager.isExcelUploadSelected(data)) {
            // TODO: Handle Excel file upload
            // This will be handled by the UI component passing file data
            println("📤 Excel upload mode selected")
            return false
        } else {
            // Manual crew entry
            val crewData = navigationLicenseManager.parseCrewFromFormData(data)

            if (crewData.isNotEmpty()) {
                val result = navigationLicenseManager.addCrewBulkIssue(requestId, crewData)

                if (result.isFailure) {
                    val error = result.exceptionOrNull()
                    println("❌ Failed to add crew: ${error?.message}")

                    // ✅ Parse error from message format: "API Error 406: {json}"
                    val errorMessage = error?.message ?: ""

                    // Try to extract error code and message from "API Error XXX: {json}" format
                    val errorCodeRegex = """API Error (\d+):\s*(.*)""".toRegex()
                    val match = errorCodeRegex.find(errorMessage)

                    if (match != null) {
                        val code = match.groupValues[1] // e.g., "406"
                        val jsonPart = match.groupValues[2] // e.g., {"timestamp":"...","message":"..."}

                        // Try to parse JSON to extract the message
                        val messageFromJson = try {
                            // Simple extraction of "message" field from JSON
                            val messageRegex = """"message"\s*:\s*"([^"]*)"""".toRegex()
                            val messageMatch = messageRegex.find(jsonPart)
                            messageMatch?.groupValues?.get(1) ?: jsonPart
                        } catch (e: Exception) {
                            jsonPart
                        }

                        // Store in formData for UI to display
                        accumulatedFormData["apiErrorCode"] = code
                        accumulatedFormData["apiErrorMessage"] = messageFromJson

                        println("🔍 Extracted error code: $code, message: $messageFromJson")

                        // If it's not a 406 error, show generic dialog instead
                        if (code != "406") {
                            accumulatedFormData["apiError"] = errorMessage
                        } else {
                            // For 406 errors, also set apiError to trigger the dialog
                            accumulatedFormData["apiError"] = messageFromJson
                        }
                    } else {
                        // Fallback: store the raw error message
                        accumulatedFormData["apiError"] = errorMessage
                    }

                    // ✅ Return true to indicate error occurred
                    return true
                }

                println("✅ Added ${crewData.size} crew members successfully")
            }
        }

        return false
    }

    /**
     * Ensure navigation request is created before submitting data
     * @return Request ID if successful
     */
    private suspend fun ensureRequestCreated(): Long? {
        if (navigationRequestId != null) {
            return navigationRequestId
        }

        // Get selected ship info ID from accumulated data
        val selectedUnitsJson = accumulatedFormData["selectedMarineUnits"]

        // Parse JSON to extract shipInfoId
        val shipInfoId = if (!selectedUnitsJson.isNullOrEmpty() && selectedUnitsJson != "[]") {
            try {
                // Handle two possible formats:
                // 1. Array of IDs: ["132435445"]
                // 2. Array of objects: [{"id":"123","shipName":"..."}]

                // First try: simple array of strings/numbers ["132435445"]
                val simpleArrayRegex = """\["?(\d+)"?\]""".toRegex()
                val simpleMatch = simpleArrayRegex.find(selectedUnitsJson)

                if (simpleMatch != null) {
                    simpleMatch.groupValues[1].toLongOrNull()
                } else {
                    // Second try: array of objects with id field
                    val objectIdRegex = """"id"\s*:\s*"?(\d+)"?""".toRegex()
                    val objectMatch = objectIdRegex.find(selectedUnitsJson)
                    objectMatch?.groupValues?.get(1)?.toLongOrNull()
                }
            } catch (e: Exception) {
                println("❌ Failed to parse selectedMarineUnits JSON: ${e.message}")
                null
            }
        } else {
            null
        }

        if (shipInfoId == null) {
            println("❌ No ship selected, cannot create request")
            println("🔍 selectedUnitsJson = $selectedUnitsJson")
            return null
        }

        println("✅ Extracted shipInfoId: $shipInfoId from JSON: $selectedUnitsJson")

        // Create the request
        val result = navigationLicenseManager.createIssueRequest(shipInfoId)

        if (result.isFailure) {
            val error = result.exceptionOrNull()
            println("❌ Failed to create navigation license request: ${error?.message}")

            // ✅ Parse error from message format: "API Error 406: {json}"
            val errorMessage = error?.message ?: ""

            // Try to extract error code and message from "API Error XXX: {json}" format
            val errorCodeRegex = """API Error (\d+):\s*(.*)""".toRegex()
            val match = errorCodeRegex.find(errorMessage)

            if (match != null) {
                val code = match.groupValues[1] // e.g., "406"
                val jsonPart = match.groupValues[2] // e.g., {"timestamp":"...","message":"..."}

                // Try to parse JSON to extract the message
                val messageFromJson = try {
                    // Simple extraction of "message" field from JSON
                    val messageRegex = """"message"\s*:\s*"([^"]*)"""".toRegex()
                    val messageMatch = messageRegex.find(jsonPart)
                    messageMatch?.groupValues?.get(1) ?: jsonPart
                } catch (e: Exception) {
                    jsonPart
                }

                // Store in formData for UI to display
                accumulatedFormData["apiErrorCode"] = code
                accumulatedFormData["apiErrorMessage"] = messageFromJson

                println("🔍 Extracted error code: $code, message: $messageFromJson")

                // If it's not a 406 error, show generic dialog instead
                if (code != "406") {
                    accumulatedFormData["apiError"] = errorMessage
                } else {
                    // For 406 errors, also set apiError to trigger the dialog
                    accumulatedFormData["apiError"] = messageFromJson
                }
            } else {
                // Fallback: store the raw error message
                accumulatedFormData["apiError"] = errorMessage
            }

            return null
        }

        // Success case
        navigationRequestId = result.getOrNull()
        println("✅ Navigation license request created with ID: $navigationRequestId")

        return navigationRequestId
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        // Final submission - all data has been submitted step by step
        println("✅ Issue Navigation Permit - All data submitted successfully")
        return Result.success(true)
    }

    override fun getFormData(): Map<String, String> {
        return accumulatedFormData.toMap()
    }

    override fun handleFieldChange(fieldId: String, value: String, formData: Map<String, String>): Map<String, String> {
        if (fieldId == "owner_type") {
            val mutableFormData = formData.toMutableMap()
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
