package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.R
import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.transactions.shared.DocumentConfig
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.business.transactions.managers.NavigationLicenseManager
import com.informatique.mtcit.business.transactions.shared.StepType
import com.informatique.mtcit.data.model.NavigationArea
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.data.repository.MarineUnitRepository
import com.informatique.mtcit.data.dto.CrewResDto
import com.informatique.mtcit.data.dto.NavigationAreaResDto
import com.informatique.mtcit.navigation.NavRoutes
import com.informatique.mtcit.navigation.NavigationManager
import com.informatique.mtcit.ui.components.PersonType
import com.informatique.mtcit.ui.components.SelectableItem
import com.informatique.mtcit.ui.repo.CompanyRepo
import com.informatique.mtcit.ui.screens.RequestDetail.CheckShipCondition
import com.informatique.mtcit.ui.viewmodels.StepData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.informatique.mtcit.util.UserHelper


/**
 * Strategy for Renew Navigation Permit
 * Uses NavigationLicenseManager for all navigation license operations
 * Key difference from Issue: Loads existing navigation areas and crew from previous license
 */
class RenewNavigationPermitStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val companyRepository: CompanyRepo,
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository,
    private val marineUnitRepository: MarineUnitRepository,
    private val navigationManager: NavigationManager,
    private val navigationLicenseManager: NavigationLicenseManager,
    private val shipSelectionManager: com.informatique.mtcit.business.transactions.shared.ShipSelectionManager,
    @ApplicationContext private val appContext: Context
    ) : TransactionStrategy {
    private var countryOptions: List<String> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()
    private var commercialOptions: List<SelectableItem> = emptyList()
    private var typeOptions: List<PersonType> = emptyList()
    private var sailingRegionsOptions: List<NavigationArea> = emptyList()
    private var crewJobTitles: List<String> = emptyList()
    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf()

    private var navigationRequestId: Long? = null // ✅ Store created request ID
    private var lastNavLicId: Long? = null // ✅ Store last navigation license ID
    private var existingNavigationAreas: List<NavigationAreaResDto> = emptyList() // ✅ Loaded areas
    private var existingCrew: List<CrewResDto> = emptyList() // ✅ Loaded crew

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        // ✅ Get civilId from token
        val ownerCivilId = UserHelper.getOwnerCivilId(appContext)
        println("🔑 Owner CivilId from token: $ownerCivilId")

        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val commercialRegistrations = lookupRepository.getCommercialRegistrations(ownerCivilId).getOrNull() ?: emptyList()
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()

        println("🚢 Skipping initial ship load - will load after user selects type and presses Next")

        countryOptions = countries
        commercialOptions = commercialRegistrations
        typeOptions = personTypes

        return mapOf(
            "marineUnits" to emptyList<MarineUnit>(),
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
                    // ✅ Load existing navigation areas for renew
                    loadExistingNavigationAreas()
                }
                "crewJobTitles" -> {
                    if (crewJobTitles.isEmpty()) {
                        val jobs = lookupRepository.getCrewJobTitles().getOrNull() ?: emptyList()
                        crewJobTitles = jobs
                    }
                    // ✅ Load existing crew for renew
                    loadExistingCrew()
                }
                // add other lookups if needed
            }
        }

        // Notify UI to refresh steps so dropdown picks up new data
        onStepsNeedRebuild?.invoke()
    }

    override fun getContext(): TransactionContext {
        TODO("Not yet implemented")
    }

    /**
     * ✅ Load existing navigation areas from previous license
     */
    private suspend fun loadExistingNavigationAreas() {
        if (existingNavigationAreas.isNotEmpty()) return // Already loaded

        val requestId = navigationRequestId ?: return

        navigationLicenseManager.loadNavigationAreasRenew(requestId)
            .onSuccess { areas ->
                existingNavigationAreas = areas
                println("✅ Loaded ${areas.size} existing navigation areas")
            }
            .onFailure { error ->
                println("❌ Failed to load existing navigation areas: ${error.message}")
            }
    }

    /**
     * ✅ Load existing crew from previous license
     */
    private suspend fun loadExistingCrew() {
        if (existingCrew.isNotEmpty()) return // Already loaded

        val lastLicId = lastNavLicId ?: return

        navigationLicenseManager.loadCrewRenew(lastLicId)
            .onSuccess { crew ->
                existingCrew = crew
                println("✅ Loaded ${crew.size} existing crew members")
            }
            .onFailure { error ->
                println("❌ Failed to load existing crew: ${error.message}")
            }
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

        marineUnits = marineUnitRepository.loadShipsForOwner(
            ownerCivilId = ownerCivilId,
            commercialRegNumber = commercialRegNumber,
            // **********************************************************************************************************
            //Request Type Id
            requestTypeId = TransactionType.RENEW_NAVIGATION_PERMIT.toRequestTypeId() // ✅ Renew Navigation Permit ID
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
        println("📦 RenewNavigationPermit - Updated accumulated data: $accumulatedFormData")
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
                allowMultipleSelection = false,
                showAddNewButton = true,
                showOwnedUnitsWarning = true
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

        // ✅ Handle marine unit selection (existing ship) to capture requestId from proceed-request
        if (stepData?.titleRes == R.string.owned_ships) {
            val isAddingNew = accumulatedFormData["isAddingNewUnit"]?.toBoolean() ?: false
            val selectedUnitsJson = data["selectedMarineUnits"] ?: accumulatedFormData["selectedMarineUnits"]
            val hasSelectedExistingShip = !selectedUnitsJson.isNullOrEmpty() && selectedUnitsJson != "[]" && !isAddingNew

            if (hasSelectedExistingShip) {
                try {
                    val result = shipSelectionManager.handleShipSelection(
                        shipId = selectedUnitsJson,
                        context = TransactionType.RENEW_NAVIGATION_PERMIT.context
                    )
                    when (result) {
                        is com.informatique.mtcit.business.transactions.shared.ShipSelectionResult.Success -> {
                            accumulatedFormData["requestId"] = result.requestId.toString()
                            navigationRequestId = result.requestId.toLong()
                        }
                        is com.informatique.mtcit.business.transactions.shared.ShipSelectionResult.Error -> {
                            accumulatedFormData["apiError"] = result.message
                            return -1
                        }
                    }
                } catch (e: Exception) {
                    accumulatedFormData["apiError"] = e.message ?: "فشل في متابعة الطلب"
                    return -1
                }
            }
        }

        // ✅ Use stepType instead of checking field IDs
        when (stepData?.stepType) {
            StepType.NAVIGATION_AREAS -> handleNavigationAreasSubmission(data)
            StepType.CREW_MANAGEMENT -> handleCrewSubmission(data)
            else -> {}
        }

        if (step == 0 && data.filterValues { it == "فرد" }.isNotEmpty()){
            return 2
        } else if (step == 2 && data.filterValues { it == "[\"470123456\"]" }.isNotEmpty()){
            // ✅ TODO: Uncomment after backend integration is complete
            // This forwards to RequestDetailScreen when compliance issues are detected
            /*
            navigationManager.navigate(NavRoutes.RequestDetailRoute.createRoute(
                CheckShipCondition(shipData = "")
            ))
            return -1
            */
            // ✅ For now, continue normal flow
            return step
        }
        return step
    }

    /**
     * Handle navigation areas submission (update existing or add new)
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
        val requestId = ensureRequestCreated()

        if (requestId != null) {
            // ✅ For renew: use UPDATE instead of ADD if areas exist
            if (existingNavigationAreas.isNotEmpty()) {
                navigationLicenseManager.updateNavigationAreasRenew(requestId, selectedAreaIds)
                    .onSuccess {
                        println("✅ Navigation areas updated successfully")
                    }
                    .onFailure { error ->
                        println("❌ Failed to update navigation areas: ${error.message}")
                    }
            } else {
                navigationLicenseManager.addNavigationAreasRenew(requestId, selectedAreaIds)
                    .onSuccess {
                        println("✅ Navigation areas added successfully")
                    }
                    .onFailure { error ->
                        println("❌ Failed to add navigation areas: ${error.message}")
                    }
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
            println("📤 Excel upload mode selected")
        } else {
            // Manual crew entry
            val crewData = navigationLicenseManager.parseCrewFromFormData(data)

            if (crewData.isNotEmpty()) {
                // ✅ For renew: Add new crew members (existing ones are already loaded)
                navigationLicenseManager.addCrewBulkRenew(requestId, crewData)
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

        // If requestId already captured from proceed-request, reuse it
        accumulatedFormData["requestId"]?.toLongOrNull()?.let {
            navigationRequestId = it
            return navigationRequestId
        }

        // Get selected ship info ID and last nav lic ID from accumulated data
        val shipInfoId = accumulatedFormData["selectedMarineUnit"]?.toLongOrNull()
        val lastLicId = accumulatedFormData["lastNavLicId"]?.toLongOrNull() // TODO: Get from selected ship

        if (shipInfoId == null || lastLicId == null) {
            println("❌ Missing shipInfoId or lastNavLicId, cannot create renewal request")
            return null
        }

        // Create the renewal request
        navigationLicenseManager.createRenewalRequest(shipInfoId, lastLicId)
            .onSuccess { (requestId, licId) ->
                navigationRequestId = requestId
                lastNavLicId = licId
                accumulatedFormData["requestId"] = requestId.toString()
                println("✅ Navigation license renewal request created with ID: $requestId")
            }
            .onFailure { error ->
                println("❌ Failed to create navigation license renewal request: ${error.message}")
            }

        return navigationRequestId
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        // Final submission - all data has been submitted step by step
        println("✅ Renew Navigation Permit - All data submitted successfully")
        return Result.success(true)
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
