package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.R
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
import com.informatique.mtcit.data.dto.CrewResDto
import com.informatique.mtcit.data.dto.NavigationAreaResDto
import com.informatique.mtcit.navigation.NavigationManager
import com.informatique.mtcit.ui.components.PersonType
import com.informatique.mtcit.ui.components.SelectableItem
import com.informatique.mtcit.ui.repo.CompanyRepo
import com.informatique.mtcit.ui.viewmodels.StepData
import com.informatique.mtcit.common.FormField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.informatique.mtcit.util.UserHelper

// Added imports for API error handling and message extraction
import com.informatique.mtcit.common.ApiException
import com.informatique.mtcit.common.ErrorMessageExtractor


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

    private val requestTypeId = TransactionType.RENEW_NAVIGATION_PERMIT.toRequestTypeId()
    private val transactionContext: TransactionContext = TransactionType.RENEW_NAVIGATION_PERMIT.context


    private var navigationRequestId: Long? = null // ✅ Store created request ID
    private var lastNavLicId: Long? = null // ✅ Store last navigation license ID
    private var existingNavigationAreas: List<NavigationAreaResDto> = emptyList() // ✅ Loaded areas
    private var existingCrew: List<CrewResDto> = emptyList() // ✅ Loaded crew

    // ✅ Add lastApiError and apiResponses to mirror Mortgage strategy
    private var lastApiError: String? = null
    private val apiResponses: MutableMap<String, Any> = mutableMapOf()

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        // ✅ Get civilId from token
        val ownerCivilId = UserHelper.getOwnerCivilId(appContext)
        println("🔑 Owner CivilId from token: $ownerCivilId")

        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()

        // ✅ Handle null civilId - return empty list if no token
        val commercialRegistrations = if (ownerCivilId != null) {
            lookupRepository.getCommercialRegistrations(ownerCivilId).getOrNull() ?: emptyList()
        } else {
            emptyList()
        }
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

        // Ensure sailingRegions lookup is loaded - needed for mapping ids -> names
        if (sailingRegionsOptions.isEmpty()) {
            val lookupAreas = lookupRepository.getNavigationAreas().getOrNull() ?: emptyList()
            sailingRegionsOptions = lookupAreas
            println("🔁 Loaded sailingRegions lookup inside loadExistingNavigationAreas: ${sailingRegionsOptions.map { it.id }}")
        }

        // Prefer explicit lastNavLicId (previous license id) when loading existing areas
        val lastLicId = lastNavLicId ?: accumulatedFormData["lastNavLicId"]?.toLongOrNull()
        if (lastLicId == null) {
            println("⚠️ No lastNavLicId available - cannot load existing navigation areas")
            return
        }

        navigationLicenseManager.loadNavigationAreasRenew(lastLicId)
            .onSuccess { areas ->
                existingNavigationAreas = areas
                println("✅ Loaded ${areas.size} existing navigation areas for lastNavLicId=$lastLicId")

                // Auto-select these areas in the sailingRegions step by storing the JSON array of names
                try {
                    println("🔍 Available sailingRegions lookup: ${sailingRegionsOptions.map { it.id.toString() + ':' + it.nameAr }}")
                    println("🔍 API returned areas: ${areas.map { it.id.toString() + ':' + (try { it.areaNameAr } catch (_: Exception) { "<no-name>" })}}")
                    if (areas.isNotEmpty()) {
                        // Map API-returned areas (which may use `nameAr`) to our lookup names
                        val selectedNames = areas.mapNotNull { apiArea ->
                            // apiArea.id may be Long, sailingRegionsOptions use Int ids
                            val apiId = try { apiArea.id.toInt() } catch (_: Exception) { null }
                            apiId?.let { id ->
                                sailingRegionsOptions.firstOrNull { it.id == id }?.nameAr
                            }
                        }.distinct()

                        if (selectedNames.isNotEmpty()) {
                            val namesJson = selectedNames.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
                            accumulatedFormData["sailingRegions"] = namesJson
                            println("📝 Pre-populated sailingRegions with names from lookup: $namesJson")
                            // Notify UI to rebuild steps so the selection will show
                            onStepsNeedRebuild?.invoke()
                        } else {
                            // Fallback: try mapping by Arabic name returned by API (areaNameAr)
                            val fallbackNames = areas.mapNotNull { apiArea ->
                                val nameAr = try { apiArea.areaNameAr } catch (_: Exception) { null }
                                nameAr?.let { apiName ->
                                    sailingRegionsOptions.firstOrNull { it.nameAr == apiName }?.nameAr
                                }
                            }.distinct()

                            if (fallbackNames.isNotEmpty()) {
                                val namesJson = fallbackNames.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
                                accumulatedFormData["sailingRegions"] = namesJson
                                println("📝 Pre-populated sailingRegions with fallback names from API: $namesJson")
                                onStepsNeedRebuild?.invoke()
                            } else {
                                ("⚠️ Could not map API areas to local lookup names - selected IDs: ${areas.map { it.id }}")
                            }
                        }
                    }
                } catch (e: Exception) { println("⚠️ Failed to pre-populate sailingRegions: ${e.message}")
            }
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
        // Build sailing regions step and inject any pre-populated selection from accumulatedFormData
        val sailingStep = SharedSteps.sailingRegionsStep(
            sailingRegions = sailingRegionsOptions.map { it.nameAr }
        )
        val prepopValue = accumulatedFormData["sailingRegions"]
        if (!prepopValue.isNullOrBlank()) {
            val modifiedFields = sailingStep.fields.map { field ->
                // If this is the multiselect field, set its value to the prepopulated JSON
                if (field.id == "sailingRegions" && field is FormField.MultiSelectDropDown) {
                    field.copy(value = prepopValue)
                } else field
            }
            steps.add(sailingStep.copy(fields = modifiedFields))
        } else {
            steps.add(sailingStep)
        }
        steps.add( SharedSteps.sailorInfoStep(
            includeUploadFile = false,
            includeDownloadFile = false,
            jobs = crewJobTitles
        ))

        // Review Step (shows all collected data)
        steps.add(SharedSteps.reviewStep())

        println("📋 Total steps count: ${steps.size}")
        return steps
    }

    // Expose current accumulated form data so ViewModel can merge it into UI state
    override fun getFormData(): Map<String, String> {
        return accumulatedFormData.toMap()
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

        // Clear previous API error
        lastApiError = null

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
                            // Store created request id
                            accumulatedFormData["requestId"] = result.requestId.toString()
                            navigationRequestId = result.requestId.toLong()

                            // Store full API response for later use
                            apiResponses["proceedRequest"] = result.response

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

                            // Persist maritime identification fields if available
                            result.imoNumber?.let { accumulatedFormData["imoNumber"] = it }
                            result.mmsiNumber?.let { accumulatedFormData["mmsiNumber"] = it }
                            result.callSign?.let { accumulatedFormData["callSign"] = it }

                            // Flag to indicate maritime ID step necessity
                            accumulatedFormData["needsMaritimeIdentification"] = result.needsMaritimeIdentification.toString()

                            // -----------------------
                            // Create renewal request using the simpler API (only shipInfo)
                            // This mirrors the Issue flow where createIssueRequest is called after proceed-request
                            // -----------------------
                            val shipInfoIdLong = selectedUnits?.toLongOrNull()
                            if (shipInfoIdLong != null) {
                                try {
                                    val createRes = navigationLicenseManager.createRenewalRequestSimple(shipInfoIdLong)
                                    createRes.onSuccess { createdDto ->
                                        // Store the real requestId returned by backend
                                        navigationRequestId = createdDto.id
                                        accumulatedFormData["requestId"] = createdDto.id.toString()
                                        // also store lastNavLicId if returned
                                        createdDto.lastNavLicId?.let {
                                            accumulatedFormData["lastNavLicId"] = it.toString()
                                            lastNavLicId = it
                                        }
                                        apiResponses["createRenewalRequest"] = createdDto
                                        println("✅ Renewal request created (simple) with ID: ${createdDto.id}")
                                    }

                                    createRes.onFailure { err ->
                                        val msg = err.message ?: "فشل في إنشاء طلب تجديد"
                                        lastApiError = msg
                                        println("❌ createRenewalRequestSimple failed: $msg")
                                        throw ApiException(500, msg)
                                    }

                                    // After creating renewal request, try to immediately load existing navigation areas
                                    // so they appear pre-selected without waiting for the user to open the step.
                                    if (lastNavLicId != null) {
                                        try {
                                            loadExistingNavigationAreas()
                                        } catch (e: Exception) {
                                            println("⚠️ Failed to load existing navigation areas immediately: ${e.message}")
                                        }
                                    }

                                } catch (e: com.informatique.mtcit.common.ApiException) {
                                    lastApiError = e.message
                                    throw e
                                } catch (e: Exception) {
                                    val msg = ErrorMessageExtractor.extract(e.message)
                                    lastApiError = msg
                                    throw com.informatique.mtcit.common.ApiException(500, msg)
                                }
                            }
                            // -----------------------
                        }
                        is com.informatique.mtcit.business.transactions.shared.ShipSelectionResult.Error -> {
                            // Mirror Mortgage behavior: store and throw ApiException to surface error banner
                            lastApiError = result.message
                            throw ApiException(500, result.message)
                        }
                    }
                } catch (e: ApiException) {
                    // Re-throw after storing for UI
                    lastApiError = e.message ?: "خطأ في النداء"
                    throw e
                } catch (e: Exception) {
                    println("❌ Exception in ship selection: ${e.message}")
                    val errorMsg = ErrorMessageExtractor.extract(e.message)
                    lastApiError = errorMsg
                    throw ApiException(500, errorMsg)
                }
            }
        }

        // ✅ Use stepType instead of checking field IDs
        when (stepData?.stepType) {
            StepType.NAVIGATION_AREAS -> handleNavigationAreasSubmission(data)
            StepType.CREW_MANAGEMENT -> handleCrewSubmission(data)
            else -> {}
        }

        // If we just completed the Person Type step, and the selection was "فرد" (individual),
        // navigate to the dynamically-computed marine unit selection step instead of hardcoding indices.
        if (step == 0) {
            val incomingPersonType = data["selectionPersonType"]
            val currentPersonType = incomingPersonType ?: accumulatedFormData["selectionPersonType"]
            if (currentPersonType == "فرد") {
                val stepsList = getSteps()
                val marineStepIndex = stepsList.indexOfFirst { it.titleRes == R.string.owned_ships }
                return if (marineStepIndex >= 0) marineStepIndex else step + 1
            }
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

        // ✅ REVIEW STEP - Use inspection-preview as the final submission API
        val reviewStepData = getSteps().getOrNull(step)
        if (reviewStepData?.titleRes == R.string.review) {
            println("📋 REVIEW STEP - Processing for Renew Navigation Permit")

            try {
                // ✅ STEP 1: Check inspection status using inspection-preview API
                val shipInfoIdString = accumulatedFormData["shipInfoId"]
                    ?: accumulatedFormData["coreShipsInfoId"]
                    ?: accumulatedFormData["selectedMarineUnit"]
                    ?: throw com.informatique.mtcit.common.ApiException(400, "معرف السفينة غير موجود")

                println("🔍 Extracted shipInfoId from formData: $shipInfoIdString")

                // ✅ Clean the ship ID (remove array brackets if present)
                val shipInfoId = when {
                    shipInfoIdString.startsWith("[\"") && shipInfoIdString.endsWith("\"]") -> {
                        // Array format: ["1674"] -> extract the number
                        shipInfoIdString.substring(2, shipInfoIdString.length - 2).toIntOrNull()
                            ?: throw com.informatique.mtcit.common.ApiException(400, "تنسيق معرف السفينة غير صحيح")
                    }
                    shipInfoIdString.startsWith("[") -> {
                        // Array format: ["1674"] -> extract the number
                        shipInfoIdString.trim('[', ']', '"').toIntOrNull()
                            ?: throw com.informatique.mtcit.common.ApiException(400, "تنسيق معرف السفينة غير صحيح")
                    }
                    else -> {
                        // Single value: "1674"
                        shipInfoIdString.toIntOrNull()
                            ?: throw com.informatique.mtcit.common.ApiException(400, "معرف السفينة غير صحيح")
                    }
                }

                val requestId = navigationRequestId
                if (requestId == null) {
                    throw Exception("No navigation request ID available. Ship selection might have failed.")
                }

                println("   Calling checkInspectionPreview with shipInfoId: $requestId")
                val inspectionResult = marineUnitRepository.checkInspectionPreview(requestId.toInt(), transactionContext.inspectionPreviewBaseContext)

                // ✅ Handle inspection status - inspection-preview IS the send-request for navigation licenses
                inspectionResult.fold(
                    onSuccess = { inspectionStatus ->
                        println("✅ Inspection preview check successful")
                        println("   Inspection status: $inspectionStatus (0=no inspection, 1=has inspection)")

                        if (inspectionStatus == 0) {
                            // ✅ Ship requires inspection - Show inspection dialog
                            println("⚠️ Ship requires inspection - showing inspection dialog")

                            // Show inspection required dialog
                            accumulatedFormData["showInspectionDialog"] = "true"
                            accumulatedFormData["inspectionMessage"] =
                                "السفينة تحتاج إلى معاينة قبل إكمال الإجراءات. يرجى تقديم طلب معاينة أولاً."

                            return -1 // Block navigation

                        } else {
                            // ✅ Inspection done (data=1) - Show success dialog
                            println("✅ Ship has inspection completed - request submitted successfully")

                            // ✅ For navigation licenses, inspection-preview IS the send-request API
                            // No need to call separate send-request endpoint

                            val requestNumber = accumulatedFormData["requestSerial"]
                                ?: accumulatedFormData["requestId"]
                                ?: "N/A"

                            // ✅ NEW: Check if this is a NEW request (not resumed)
                            val isNewRequest = accumulatedFormData["isResumedTransaction"]?.toBoolean() != true

                            println("🔍 isNewRequest check:")
                            println("   - isResumedTransaction flag: ${accumulatedFormData["isResumedTransaction"]}")
                            println("   - isNewRequest result: $isNewRequest")

                            if (isNewRequest) {
                                println("🎉 NEW request submitted - showing success dialog and stopping")

                                // Set success flags for ViewModel to show dialog
                                accumulatedFormData["requestSubmitted"] = "true"
                                accumulatedFormData["requestNumber"] = requestNumber
                                accumulatedFormData["successMessage"] = "تم إرسال الطلب بنجاح"
                                accumulatedFormData["needInspection"] = "false"

                                // Return -2 to indicate: success but show dialog and stop
                                return -2
                            }

                            // ✅ For resumed requests: Show success dialog
                            println("✅ Showing success dialog for resumed request")
                            accumulatedFormData["showSuccessAlert"] = "true"
                            accumulatedFormData["successAlertMessage"] = "تم إرسال الطلب بنجاح"

                            return step // Stay on current step to show alert
                        }
                    },
                    onFailure = { error ->
                        println("❌ Failed to check inspection preview: ${error.message}")
                        // On error, show error message and block
                        accumulatedFormData["apiError"] =
                            "حدث خطأ أثناء التحقق من المعاينة: ${error.message}"
                        return -1 // Block navigation
                    }
                )

                // ✅ Unreachable - kept for compilation
            } catch (e: Exception) {
                println("❌ Exception in review step: ${e.message}")
                e.printStackTrace()
                accumulatedFormData["apiError"] =
                    "حدث خطأ أثناء إرسال الطلب: ${e.message}"
                return -1
            }
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
                navigationLicenseManager.addCrewBulkRenew(requestId,
                    crewData as List<Map<String, String>>
                )
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

    // Expose last API error to UI similar to Mortgage strategy
    fun getLastApiError(): String? = lastApiError

    fun clearLastApiError() {
        lastApiError = null
    }

    // Store API responses for later retrieval (e.g., proceedRequest response)
    override fun storeApiResponse(apiName: String, response: Any) {
        println("💾 Storing API response for '$apiName': $response")
        apiResponses[apiName] = response
    }

    override fun getApiResponse(apiName: String): Any? {
        return apiResponses[apiName]
    }
}
