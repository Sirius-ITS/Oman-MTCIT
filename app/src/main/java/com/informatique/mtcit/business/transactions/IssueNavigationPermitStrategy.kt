package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.R
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
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.informatique.mtcit.util.UserHelper

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
    private val shipSelectionManager: com.informatique.mtcit.business.transactions.shared.ShipSelectionManager,
    private val paymentManager: PaymentManager,
    @ApplicationContext private val appContext: Context
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
    override fun getContext(): TransactionContext {
        TODO("Not yet implemented")
    }

    /**
     * Load ships for the selected person type / commercial registration.
     * This mirrors the behavior in Temporary/Permanent strategies so the UI can show owned ships.
     */
    override suspend fun loadShipsForSelectedType(formData: Map<String, String>): List<MarineUnit> {
        val personType = formData["selectionPersonType"]
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
        // ✅ Get civilId from token
        val ownerCivilId = UserHelper.getOwnerCivilId(appContext)
        println("🔑 Owner CivilId from token: $ownerCivilId")

        // ✅ Don't load countries here - will be loaded in onStepOpened with ISO codes
        // ✅ Handle null civilId - return empty list if no token
        val commercialRegistrations = if (ownerCivilId != null) {
            lookupRepository.getCommercialRegistrations(ownerCivilId).getOrNull() ?: emptyList()
        } else {
            emptyList()
        }
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()

        // countryOptions will be loaded in onStepOpened() with proper ISO code format
        commercialOptions = commercialRegistrations
        typeOptions = personTypes


        return mapOf(
            "marineUnits" to emptyList<MarineUnit>(), // ✅ Empty initially
            "registrationCountry" to emptyList<String>(), // ✅ Empty - loaded lazily
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
                        // ✅ Load with IDs in "ID|Name" format
                        val jobs = lookupRepository.getCrewJobTitlesRaw()
                        crewJobTitles = jobs.map { "${it.id}|${it.nameAr}" }
                        println("✅ Loaded ${crewJobTitles.size} crew job titles with IDs")
                    }
                }
                "countries" -> {
                    if (countryOptions.isEmpty()) {
                        val countries = lookupRepository.getCountriesRaw()
                        println("🌍 Raw countries from API (first 3):")
                        countries.take(3).forEach { println("   - id='${it.id}', nameAr='${it.nameAr}', isoCode='${it.isoCode}'") }
                        // ✅ IMPORTANT: Use isoCode (ISO country code like "UA") instead of id (which contains country name)
                        countryOptions = countries.map { "${it.isoCode}|${it.nameAr}" }
                        println("✅ Loaded ${countryOptions.size} countries with ISO codes")
                        println("   First 3 formatted: ${countryOptions.take(3)}")
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
            jobs = crewJobTitles,
            nationalities = countryOptions
        ))

        // Review Step (shows all collected data)
        steps.add(SharedSteps.reviewStep())

        // Payment Details Step - pass accumulated form data
        // steps.add(SharedSteps.paymentDetailsStep(accumulatedFormData))

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

        // ✅ Handle marine unit selection - call createIssueRequest after successful ship selection
        if (stepData?.titleRes == R.string.owned_ships) {
            val isAddingNew = accumulatedFormData["isAddingNewUnit"]?.toBoolean() ?: false
            val selectedUnitsJson = data["selectedMarineUnits"]
            val hasSelectedExistingShip = !selectedUnitsJson.isNullOrEmpty() && selectedUnitsJson != "[]" && !isAddingNew

            if (hasSelectedExistingShip) {
                try {
                    // ✅ Step 1: Call selectships (proceed-request) - just for validation
                    val selectionResult = shipSelectionManager.handleShipSelection(
                        shipId = selectedUnitsJson,
                        context = TransactionType.ISSUE_NAVIGATION_PERMIT.context
                    )

                    when (selectionResult) {
                        is com.informatique.mtcit.business.transactions.shared.ShipSelectionResult.Success -> {
                            // ✅ Step 2: After successful selection, create the navigation license request
                            println("✅ Ship selection successful, now creating navigation license request...")

                            // Extract shipInfoId from selectedUnitsJson
                            val shipInfoId = extractShipInfoId(selectedUnitsJson)

                            if (shipInfoId != null) {
                                // ✅ Call createIssueRequest API to get the real requestId
                                val createResult = navigationLicenseManager.createIssueRequest(shipInfoId)

                                createResult.fold(
                                    onSuccess = { requestId ->
                                        // ✅ Store the requestId from createIssueRequest (not from selectships)
                                        navigationRequestId = requestId
                                        accumulatedFormData["requestId"] = requestId.toString()
                                        println("✅ Navigation license request created with ID: $requestId")
                                    },
                                    onFailure = { error ->
                                        println("❌ Failed to create navigation license request: ${error.message}")
                                        accumulatedFormData["apiError"] = error.message ?: "فشل في إنشاء طلب رخصة الملاحة"
                                        return -1
                                    }
                                )
                            } else {
                                accumulatedFormData["apiError"] = "فشل في استخراج معرف السفينة"
                                return -1
                            }
                        }
                        is com.informatique.mtcit.business.transactions.shared.ShipSelectionResult.Error -> {
                            accumulatedFormData["apiError"] = selectionResult.message
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
     * Extract shipInfoId from selectedMarineUnits JSON
     * Handles formats: ["132435445"] or [{"id":"123","shipName":"..."}]
     */
    private fun extractShipInfoId(selectedUnitsJson: String?): Long? {
        if (selectedUnitsJson.isNullOrEmpty() || selectedUnitsJson == "[]") {
            return null
        }

        return try {
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
            return false
        }

        println("✅ Selected navigation areas: names=$selectedNames, ids=$selectedAreaIds")

        // ✅ Use the navigationRequestId that was created after ship selection
        val requestId = navigationRequestId
        if (requestId == null) {
            throw Exception("No navigation request ID available. Ship selection might have failed.")
        }

        // ✅ Call API - let exceptions propagate to ViewModel
        navigationLicenseManager.addNavigationAreasIssue(requestId, selectedAreaIds).getOrThrow()

        println("✅ Navigation areas added successfully")
        return false
    }

    /**
     * Handle crew submission (manual or Excel)
     * @return true if error occurred, false if successful
     */
    private suspend fun handleCrewSubmission(data: Map<String, String>): Boolean {
        println("🔵 handleCrewSubmission called with data keys: ${data.keys}")

        // ✅ Use the navigationRequestId that was created after ship selection
        val requestId = navigationRequestId
        if (requestId == null) {
            println("❌ No requestId available - cannot add crew")
            throw Exception("Cannot add crew - no request ID available")
        }

        println("✅ Using requestId: $requestId")

        // Check if user chose Excel upload
        if (navigationLicenseManager.isExcelUploadSelected(data)) {
            // TODO: Handle Excel file upload
            // This will be handled by the UI component passing file data
            println("📤 Excel upload mode selected")
            return false
        } else {
            // Manual crew entry
            println("👥 Manual crew entry mode - parsing form data...")
            val crewData = navigationLicenseManager.parseCrewFromFormData(data)

            println("📋 Parsed ${crewData.size} crew members from form data")

            if (crewData.isNotEmpty()) {
                println("📤 Calling addCrewBulkIssue API with ${crewData.size} crew members...")

                // ✅ Call API - let exceptions propagate to ViewModel
                navigationLicenseManager.addCrewBulkIssue(requestId, crewData).getOrThrow()

                println("✅ Successfully added ${crewData.size} crew members")
            } else {
                println("⚠️ No crew data to submit")
            }
        }

        return false
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
