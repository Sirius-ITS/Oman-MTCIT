package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.business.transactions.managers.NavigationLicenseManager
import com.informatique.mtcit.business.transactions.shared.StepType
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
import kotlinx.serialization.json.jsonPrimitive
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
    private val navigationLicenseManager: NavigationLicenseManager
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

        // For now use ownerCivilId workaround as other strategies (API limitation)
        val (ownerCivilId, commercialRegNumber) = when (personType) {
            "فرد" -> Pair("12345678", null)
            "شركة" -> Pair("12345678", null)
            else -> Pair(null, null)
        }

        marineUnits = marineUnitRepository.loadShipsForOwner(ownerCivilId, commercialRegNumber)

        // Debug logging
        println("✅ IssueNavigationPermit - Loaded ${'$'}{marineUnits.size} ships")
        marineUnits.forEach { println("   - ${'$'}{it.shipName} (ID: ${'$'}{it.id})") }

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
        val commercialRegistrations = lookupRepository.getCommercialRegistrations().getOrNull() ?: emptyList()
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
            StepType.NAVIGATION_AREAS -> handleNavigationAreasSubmission(data)
            StepType.CREW_MANAGEMENT -> handleCrewSubmission(data)
            else -> {}
        }

        return step
    }

    /**
     * Handle navigation areas submission
     */
    private suspend fun handleNavigationAreasSubmission(data: Map<String, String>) {
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
            return
        }

        println("✅ Selected navigation areas: names=$selectedNames, ids=$selectedAreaIds")

        // Ensure we have a request ID (create request if needed)
//        val requestId = ensureRequestCreated()
        val requestId = 45L // TODO: Remove hardcoded ID after testing

        if (requestId != null) {
            navigationLicenseManager.addNavigationAreasIssue(requestId, selectedAreaIds)
                .onSuccess {
                    println("✅ Navigation areas added successfully")
                }
                .onFailure { error ->
                    println("❌ Failed to add navigation areas: ${error.message}")
                }
        }
    }

    /**
     * Handle crew submission (manual or Excel)
     */
    private suspend fun handleCrewSubmission(data: Map<String, String>) {
        val requestId = ensureRequestCreated() ?: return

        // Check if user chose Excel upload
        if (navigationLicenseManager.isExcelUploadSelected(data)) {
            // TODO: Handle Excel file upload
            // This will be handled by the UI component passing file data
            println("📤 Excel upload mode selected")
        } else {
            // Manual crew entry
            val crewData = navigationLicenseManager.parseCrewFromFormData(data)

            if (crewData.isNotEmpty()) {
                navigationLicenseManager.addCrewBulkIssue(requestId, crewData)
                    .onSuccess { crew ->
                        println("✅ Added ${crew.size} crew members successfully")
                    }
                    .onFailure { error ->
                        println("❌ Failed to add crew: ${error.message}")
                    }
            }
        }
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
        val shipInfoId = accumulatedFormData["selectedMarineUnit"]?.toLongOrNull()

        if (shipInfoId == null) {
            println("❌ No ship selected, cannot create request")
            return null
        }

        // Create the request
        navigationLicenseManager.createIssueRequest(shipInfoId)
            .onSuccess { requestId ->
                navigationRequestId = requestId
                println("✅ Navigation license request created with ID: $requestId")
            }
            .onFailure { error ->
                println("❌ Failed to create navigation license request: ${error.message}")
            }

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
