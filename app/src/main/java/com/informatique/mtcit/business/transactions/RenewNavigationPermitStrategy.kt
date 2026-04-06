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
import com.informatique.mtcit.business.transactions.shared.PaymentManager
import com.informatique.mtcit.business.transactions.shared.StepProcessResult
import com.informatique.mtcit.business.validation.rules.FormatValidationRules
import com.informatique.mtcit.business.validation.rules.ValidationRule
import dagger.hilt.android.qualifiers.ApplicationContext
import com.informatique.mtcit.util.UserHelper
import com.informatique.mtcit.ui.components.SailorData

// Added imports for API error handling and message extraction
import com.informatique.mtcit.common.ApiException
import com.informatique.mtcit.common.ErrorMessageExtractor
import com.informatique.mtcit.common.util.AppLanguage


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
    private val paymentManager: PaymentManager,
    private val inspectionFlowManager: com.informatique.mtcit.business.transactions.shared.InspectionFlowManager,  // ✅ NEW: Inspection flow manager
    @ApplicationContext private val appContext: Context
    ) : BaseTransactionStrategy() {
    private var countryOptions: List<String> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()
    private var commercialOptions: List<SelectableItem> = emptyList()
    private var typeOptions: List<PersonType> = emptyList()
    private var sailingRegionsOptions: List<NavigationArea> = emptyList()
    private var crewJobTitles: List<String> = emptyList()
    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf()
    private val requestTypeId = TransactionType.RENEW_NAVIGATION_PERMIT.toRequestTypeId()
    private val transactionContext: TransactionContext = TransactionType.RENEW_NAVIGATION_PERMIT.context

    // ✅ INFINITE SCROLL: pagination state
    private var _currentShipsPage: Int = -1
    private var _isLastShipsPage: Boolean = true
    override val currentShipsPage: Int get() = _currentShipsPage
    override val isLastShipsPage: Boolean get() = _isLastShipsPage

    // ✅ NEW: Store loaded inspection authorities and documents
    private var loadedInspectionAuthorities: List<com.informatique.mtcit.ui.components.DropdownSection> = emptyList()
    private var loadedInspectionDocuments: List<com.informatique.mtcit.data.model.RequiredDocumentItem> = emptyList()

    private var navigationRequestId: Long? = null // ✅ Store created request ID
    private var lastNavLicId: Long? = null // ✅ Store last navigation license ID
    private var existingNavigationAreas: List<NavigationAreaResDto> = emptyList() // ✅ Loaded areas
    private var existingCrew: List<CrewResDto> = emptyList() // ✅ Loaded crew

    // ✅ Add lastApiError and apiResponses to mirror Mortgage strategy
    private var lastApiError: String? = null
    private val apiResponses: MutableMap<String, Any> = mutableMapOf()

    /**
     * ✅ Override setHasAcceptanceFromApi to also store in formData
     * This ensures the payment success dialog can access it
     */
    override fun setHasAcceptanceFromApi(hasAcceptanceValue: Int?) {
        super.setHasAcceptanceFromApi(hasAcceptanceValue)
        // ✅ Store in formData so PaymentSuccessDialog can access it
        accumulatedFormData["hasAcceptance"] = (hasAcceptanceValue == 1).toString()
        println("🔧 RenewNavigationPermitStrategy: Stored hasAcceptance in formData: ${accumulatedFormData["hasAcceptance"]}")
    }

    /**
     * ✅ NEW: Handle user clicking "Continue" on inspection required dialog
     * Load inspection lookups and inject inspection step
     */
    suspend fun handleInspectionContinue() {
        println("✅ RenewNavigationPermitStrategy.handleInspectionContinue() called")
        println("✅ User confirmed inspection requirement - loading inspection lookups...")

        val shipInfoId = accumulatedFormData["shipInfoId"]?.toIntOrNull()
            ?: accumulatedFormData["coreShipsInfoId"]?.toIntOrNull()

        if (shipInfoId == null) {
            println("❌ No shipInfoId available for inspection")
            return
        }

        println("   Using shipInfoId: $shipInfoId")

        try {
            // Load inspection lookups (purposes, places, authorities)
            val lookups = inspectionFlowManager.loadInspectionLookups(shipInfoId)

            println("✅ Inspection lookups loaded:")
            println("   - Purposes: ${lookups.purposes.size}")
            println("   - Places: ${lookups.places.size}")
            println("   - Authority sections: ${lookups.authoritySections.size}")
            println("   - Documents: ${lookups.documents.size}")

            // ✅ CRITICAL: Store authorities AND documents in member variables BEFORE setting showInspectionStep
            loadedInspectionAuthorities = lookups.authoritySections
            loadedInspectionDocuments = lookups.documents

            // Mark that inspection step should be shown
            accumulatedFormData["showInspectionStep"] = "true"
            accumulatedFormData["inspectionPurposes"] = lookups.purposes.joinToString(",")
            accumulatedFormData["inspectionPlaces"] = lookups.places.joinToString(",")

            // Clear dialog flag
            accumulatedFormData.remove("showInspectionDialog")

            println("✅ Inspection lookups stored in formData, triggering steps rebuild")
            // Trigger step rebuild to inject inspection step
            onStepsNeedRebuild?.invoke()
        } catch (e: Exception) {
            println("❌ Failed to load inspection lookups: ${e.message}")
            e.printStackTrace()
            accumulatedFormData["apiError"] = if (AppLanguage.isArabic) "فشل تحميل بيانات المعاينة: ${e.message}" else "Failed to load inspection data: ${e.message}"
        }
    }

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        // ✅ Get civilId from token
        val ownerCivilId = UserHelper.getOwnerCivilId(appContext)
        println("🔑 Owner CivilId from token: $ownerCivilId")

        // ✅ Load countries with full data for nationality dropdown (format: "ID|LocalizedName")
        val countriesRaw = lookupRepository.getCountriesRaw()
        val countriesFormatted = countriesRaw.map { "${it.id}|${if (AppLanguage.isArabic) it.nameAr else it.nameEn}" }

        // For backward compatibility with other parts
        val countriesNames = countriesRaw.map { if (AppLanguage.isArabic) it.nameAr else it.nameEn }

        // ✅ Handle null civilId - return empty list if no token
        val commercialRegistrations = if (ownerCivilId != null) {
            lookupRepository.getCommercialRegistrations(ownerCivilId).getOrNull() ?: emptyList()
        } else {
            emptyList()
        }
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()

        println("🚢 Skipping initial ship load - will load after user selects type and presses Next")

        countryOptions = countriesFormatted  // ✅ Store formatted version for dropdowns
        commercialOptions = commercialRegistrations
        typeOptions = personTypes

        return mapOf(
            "marineUnits" to emptyList<MarineUnit>(),
            "registrationCountry" to countriesNames,  // ✅ Return names for backward compatibility
            "commercialRegistration" to commercialRegistrations,
            "personType" to personTypes
        )
    }

    // Load lookups when a step is opened (lazy loading)
    override suspend fun onStepOpened(stepIndex: Int) {
        val step = getSteps().getOrNull(stepIndex) ?: return
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
                        val jobs = lookupRepository.getCrewJobTitlesRaw()
                        // Format as "ID|LocalizedName" for dropdown
                        crewJobTitles = jobs.map { "${it.id}|${if (AppLanguage.isArabic) it.nameAr else it.nameEn}" }
                        println("✅ Loaded ${crewJobTitles.size} crew job titles: ${crewJobTitles.take(3)}")
                    }
                    // ✅ Load existing crew for renew
                    loadExistingCrew()
                }
                "countries" -> {
                    // ✅ Countries are already loaded in loadDynamicOptions()
                    // Just log confirmation
                    println("✅ Countries already loaded: ${countryOptions.size} available")
                }
                // add other lookups if needed
            }
        }

        // Notify UI to refresh steps so dropdown picks up new data
        onStepsNeedRebuild?.invoke()
    }

    override fun getContext(): TransactionContext = transactionContext

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
                                sailingRegionsOptions.firstOrNull { it.id == id }?.let {
                                    if (AppLanguage.isArabic) it.nameAr else it.nameEn
                                }
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
                                    sailingRegionsOptions.firstOrNull { it.nameAr == apiName }?.let {
                                        if (AppLanguage.isArabic) it.nameAr else it.nameEn
                                    }
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
                val msg = when (error) {
                    is ApiException -> error.message ?: if (AppLanguage.isArabic) "فشل في تحميل مناطق الإبحار" else "Failed to load sailing areas"
                    else -> ErrorMessageExtractor.extract(error.message)
                }
                accumulatedFormData["apiError"] = msg
                lastApiError = msg
                if (error is ApiException) throw error else throw ApiException(500, msg)
            }
    }

    /**
     * ✅ Load existing crew from the RENEWAL REQUEST (not the previous license)
     *
     * ⚠️ IMPORTANT: We must load crew using `navigationRequestId` (the current renewal request,
     * e.g. 1741), NOT `lastNavLicId` (the previous license, e.g. 1721).
     *
     * The server creates new crew records for the renewal request with NEW IDs.
     * If we mistakenly use IDs from the previous license, subsequent PUT/DELETE calls will fail
     * with HTTP 406 "Crew member does not belong to navigation license request".
     */
    private suspend fun loadExistingCrew() {
        if (existingCrew.isNotEmpty()) {
            println("⚠️ Existing crew already loaded - skipping")
            return // Already loaded
        }

        // ✅ Use the current renewal REQUEST ID — server assigns new crew IDs under this request
        val requestId = navigationRequestId
            ?: accumulatedFormData["requestId"]?.toLongOrNull()

        if (requestId == null) {
            println("⚠️ No renewal requestId available — cannot load crew for renewal request")
            return
        }

        println("🔄 Loading existing crew for renewal requestId=$requestId (NOT lastNavLicId)")

        navigationLicenseManager.loadCrewRenew(requestId)
            .onSuccess { crew ->
                existingCrew = crew
                println("✅ Loaded ${crew.size} existing crew members from API")

                // ✅ Convert API crew data to SailorData JSON format for pre-population
                if (crew.isNotEmpty()) {
                    try {
                        // Convert each CrewResDto to SailorData format
                        val sailorsJsonArray = crew.map { crewMember ->
                            // Format: job = "ID|LocalizedName"
                            val jobFormatted = "${crewMember.jobTitle.id}|${if (AppLanguage.isArabic) crewMember.jobTitle.nameAr else crewMember.jobTitle.nameEn}"

                            // Format: nationality = "ID|LocalizedName"
                            val nationalityFormatted = crewMember.nationality?.let {
                                println("🌍 Crew nationality details: id=${it.id}, nameAr=${it.nameAr}, nameEn=${it.nameEn}, isoCode=${it.isoCode}, capitalAr=${it.capitalAr}")
                                "${it.id}|${if (AppLanguage.isArabic) it.nameAr else it.nameEn}"
                            } ?: ""

                            // Build JSON object string manually to match SailorData structure
                            // ✅ Use actual crew ID from API for existing sailors (not UUID)
                            """
                            {
                                "id":"${crewMember.id}",
                                "apiId":${crewMember.id},
                                "job":"$jobFormatted",
                                "nameAr":"${crewMember.nameAr}",
                                "nameEn":"${crewMember.nameEn}",
                                "identityNumber":"${crewMember.civilNo ?: ""}",
                                "seamanPassportNumber":"${crewMember.seamenBookNo}",
                                "nationality":"$nationalityFormatted",
                                "fullName":"${crewMember.nameEn}"
                            }
                            """.trimIndent().replace("\n", "").replace("  ", "")
                        }

                        // Build JSON array string
                        val sailorsJson = "[${sailorsJsonArray.joinToString(",")}]"

                        // Store in accumulated form data
                        accumulatedFormData["sailors"] = sailorsJson

                        println("📝 Pre-populated sailors field with ${crew.size} crew members")
                        println("   Sample JSON: ${sailorsJson.take(200)}...")

                        // Notify UI to rebuild steps so the crew will show
                        onStepsNeedRebuild?.invoke()

                    } catch (e: Exception) {
                        println("⚠️ Failed to pre-populate sailors field: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
            .onFailure { error ->
                println("❌ Failed to load existing crew: ${error.message}")
                val msg = when (error) {
                    is ApiException -> error.message ?: if (AppLanguage.isArabic) "فشل في تحميل بيانات الطاقم" else "Failed to load crew data"
                    else -> ErrorMessageExtractor.extract(error.message)
                }
                accumulatedFormData["apiError"] = msg
                lastApiError = msg
                if (error is ApiException) throw error else throw ApiException(500, msg)
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
            "فرد", "Individual" -> {
                println("✅ Individual: Using ownerCivilId from token")
                Pair(ownerCivilIdFromToken, null)
            }
            "شركة", "Company" -> {
                println("✅ Company: Using commercialRegNumber from selectionData = $commercialReg")
                Pair(ownerCivilIdFromToken, commercialReg) // ✅ Use civilId from token + commercialReg
            }
            else -> Pair(null, null)
        }

        println("🔍 Loading first page with loadShipsPage(page=0)")
        val firstPage = marineUnitRepository.loadShipsPage(
            ownerCivilId = ownerCivilId,
            commercialRegNumber = commercialRegNumber,
            requestTypeId = requestTypeId,
            page = 0
        )
        marineUnits = firstPage.ships
        _currentShipsPage = 0
        _isLastShipsPage = firstPage.isLastPage

        println("✅ Loaded ${marineUnits.size} ships (isLast=$_isLastShipsPage)")
        marineUnits.forEach { unit -> println("   - ${unit.shipName} (ID: ${unit.id})") }
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
        println("📄 loadNextShipsPage (RenewNavPermit) page=$nextPage")
        val result = marineUnitRepository.loadShipsPage(
            ownerCivilId = ownerCivilId,
            commercialRegNumber = commercialReg,
            requestTypeId = requestTypeId,
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
        println("📦 RenewNavigationPermit - Updated accumulated data: $accumulatedFormData")
    }

    override fun getSteps(): List<StepData> {
        val steps = mutableListOf<StepData>()
        // Step 1: Person Type
        steps.add(SharedSteps.personTypeStep(typeOptions))

        // Step 2: Commercial Registration (فقط للشركات)
        val selectedPersonType = accumulatedFormData["selectionPersonType"]
        if (selectedPersonType == "شركة" || selectedPersonType == "Company") {
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
            sailingRegions = sailingRegionsOptions.map { if (AppLanguage.isArabic) it.nameAr else it.nameEn }
        )
        val prepopValue = accumulatedFormData["sailingRegions"]
        val prepopPassengersNo = accumulatedFormData["passengersNo"]
        if (!prepopValue.isNullOrBlank() || !prepopPassengersNo.isNullOrBlank()) {
            val modifiedFields = sailingStep.fields.map { field ->
                when {
                    field.id == "sailingRegions" && field is FormField.MultiSelectDropDown && !prepopValue.isNullOrBlank() ->
                        field.copy(value = prepopValue)
                    field.id == "passengersNo" && field is FormField.TextField && !prepopPassengersNo.isNullOrBlank() ->
                        field.copy(value = prepopPassengersNo)
                    else -> field
                }
            }
            steps.add(sailingStep.copy(fields = modifiedFields))
        } else {
            steps.add(sailingStep)
        }

        // ✅ Build sailor info step and inject pre-populated crew data from accumulatedFormData
        val sailorStep = SharedSteps.sailorInfoStep(
            includeUploadFile = false,
            includeDownloadFile = true, // ✅ Must be true to show SailorList field
            jobs = crewJobTitles,
            nationalities = countryOptions  // ✅ Pass countries for nationality dropdown
        )
        val prepopSailors = accumulatedFormData["sailors"]
        if (!prepopSailors.isNullOrBlank()) {
            val modifiedSailorFields = sailorStep.fields.map { field ->
                // If this is the sailor list field, set its value to the prepopulated JSON
                if (field.id == "sailors" && field is FormField.SailorList) {
                    field.copy(
                        value = prepopSailors,
                        nationalities = countryOptions  // ✅ Also update nationalities
                    )
                } else field
            }
            steps.add(sailorStep.copy(fields = modifiedSailorFields))
        } else {
            steps.add(sailorStep)
        }

        // Review Step (shows all collected data)
        steps.add(SharedSteps.reviewStep())

        // ✅ NEW: Inspection Purpose Step (dynamically added when inspection is required)
        val showInspectionStep = accumulatedFormData["showInspectionStep"]?.toBoolean() ?: false
        if (showInspectionStep) {
            println("📋 Adding Inspection Purpose Step (dynamically injected)")

            // Parse lookups from formData
            val purposes = accumulatedFormData["inspectionPurposes"]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val places = accumulatedFormData["inspectionPlaces"]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

            println("   - Purposes: ${purposes.size}")
            println("   - Places: ${places.size}")
            println("   - Authority sections: ${loadedInspectionAuthorities.size}")
            println("   - Inspection Documents: ${loadedInspectionDocuments.size}")

            // ✅ Use inspection-specific documents
            steps.add(
                SharedSteps.inspectionPurposeAndAuthorityStep(
                    inspectionPurposes = purposes,
                    inspectionPlaces = places,
                    authoritySections = loadedInspectionAuthorities,
                    documents = loadedInspectionDocuments
                )
            )
        }

        // ✅ NEW: Payment Steps - Only show if we have requestId AND inspection is NOT required
        val hasRequestId = accumulatedFormData["requestId"] != null
        val inspectionRequired = accumulatedFormData["showInspectionDialog"]?.toBoolean() ?: false

        println("🔍 Payment step visibility check:")
        println("   hasRequestId: $hasRequestId")
        println("   inspectionRequired: $inspectionRequired")
        println("   showInspectionStep: $showInspectionStep")

        // ✅ Only show payment steps if we have requestId AND no inspection is pending
        if (hasRequestId && !inspectionRequired && !showInspectionStep) {
            println("✅ Adding payment steps")
            // Payment Details Step - Shows payment breakdown
            steps.add(SharedSteps.paymentDetailsStep(accumulatedFormData))

            // Payment Success Step - Only show if payment was successful
            val paymentSuccessful = accumulatedFormData["paymentSuccessful"]?.toBoolean() == true
            if (paymentSuccessful) {
                steps.add(SharedSteps.paymentSuccessStep())
            }
        } else {
            println("⏭️ Skipping payment steps (inspection required or in progress)")
        }

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
        val rules = getValidationRulesForStep(step, stepData)
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

        if (fieldIds.contains("passengersNo")) {
            rules.add(FormatValidationRules.numericOnly("passengersNo"))
        }

        return rules
    }

    override suspend fun processStepData(step: Int, data: Map<String, String>): Int {
        // Update accumulated data first
        accumulatedFormData.putAll(data)

        val stepData = getSteps().getOrNull(step)

        // Clear previous API error
        lastApiError = null

        // ✅ Handle marine unit selection (existing ship) to capture requestId from proceed-request
        if (stepData != null) {
            val stepType = stepData.stepType
            if (stepData.titleRes == R.string.owned_ships) {
                val isAddingNew = accumulatedFormData["isAddingNewUnit"]?.toBoolean() ?: false
                val selectedUnitsJson =
                    data["selectedMarineUnits"] ?: accumulatedFormData["selectedMarineUnits"]
                val hasSelectedExistingShip =
                    !selectedUnitsJson.isNullOrEmpty() && selectedUnitsJson != "[]" && !isAddingNew

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
                                        val shipIds = cleanJson.split(",")
                                            .map { it.trim().removeSurrounding("\"") }
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
                                accumulatedFormData["needsMaritimeIdentification"] =
                                    result.needsMaritimeIdentification.toString()

                                // -----------------------
                                // Create renewal request using the simpler API (only shipInfo)
                                // This mirrors the Issue flow where createIssueRequest is called after proceed-request
                                // -----------------------
                                val shipInfoIdLong = selectedUnits?.toLongOrNull()
                                if (shipInfoIdLong != null) {
                                    try {
                                        val createRes =
                                            navigationLicenseManager.createRenewalRequestSimple(
                                                shipInfoIdLong
                                            )
                                        createRes.onSuccess { createdDto ->
                                            // Store the real requestId returned by backend
                                            navigationRequestId = createdDto.id
                                            accumulatedFormData["requestId"] =
                                                createdDto.id.toString()
                                            // also store lastNavLicId if returned
                                            createdDto.lastNavLicId?.let {
                                                accumulatedFormData["lastNavLicId"] = it.toString()
                                                lastNavLicId = it
                                            }
                                            apiResponses["createRenewalRequest"] = createdDto
                                            println("✅ Renewal request created (simple) with ID: ${createdDto.id}")
                                        }

                                        createRes.onFailure { err ->
                                            val msg = err.message ?: if (AppLanguage.isArabic) "فشل في إنشاء طلب تجديد" else "Failed to create renewal request"
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
                        lastApiError = e.message ?: if (AppLanguage.isArabic) "خطأ في النداء" else "Call error"
                        throw e
                    } catch (e: Exception) {
                        println("❌ Exception in ship selection: ${e.message}")
                        val errorMsg = ErrorMessageExtractor.extract(e.message)
                        lastApiError = errorMsg
                        throw ApiException(500, errorMsg)
                    }
                }
            }

            // ✅ NEW: Handle Inspection Purpose Step
            if (inspectionFlowManager.isInspectionPurposeStep(stepType)) {
                println("🔍 Processing Inspection Purpose Step...")

                try {
                    val inspectionResult = inspectionFlowManager.handleInspectionPurposeStepCompletion(
                        formData = accumulatedFormData,
                        context = appContext
                    )

                    when (inspectionResult) {
                        is StepProcessResult.Success -> {
                            println("✅ Inspection request submitted successfully!")
                            println("   Message: ${inspectionResult.message}")

                            // ✅ IMPORTANT: Exit the transaction completely
                            // When inspection is submitted from within another transaction,
                            // we should show success dialog and exit (like standalone inspection transaction)

                            // Set success flags for ViewModel to show dialog
                            accumulatedFormData["inspectionRequestSubmitted"] = "true"
                            accumulatedFormData["showInspectionSuccessDialog"] = "true"
                            accumulatedFormData["inspectionSuccessMessage"] = inspectionResult.message

                            println("🎉 Inspection submitted - exiting transaction (returning -3)")

                            // Return -3 to indicate: inspection success, show dialog and exit transaction
                            return -3
                        }
                        is StepProcessResult.Error -> {
                            println("❌ Inspection request submission failed: ${inspectionResult.message}")
                            accumulatedFormData["apiError"] = inspectionResult.message
                            return -1 // Block navigation
                        }
                        is StepProcessResult.NoAction -> {
                            println("ℹ️ No action taken for inspection step")
                            // This shouldn't happen for inspection purpose step, but handle it
                        }
                    }
                } catch (e: Exception) {
                    println("❌ Exception processing inspection step: ${e.message}")
                    e.printStackTrace()
                    accumulatedFormData["apiError"] = if (AppLanguage.isArabic) "حدث خطأ أثناء إرسال طلب المعاينة: ${e.message}" else "An error occurred while submitting the inspection request: ${e.message}"
                    return -1
                }
            }

            val paymentResult = paymentManager.processStepIfNeeded(
                stepType = stepType,
                formData = accumulatedFormData,
                requestTypeId = requestTypeId.toInt(),
                context = transactionContext // ✅ Pass TransactionContext
            )

            when (paymentResult) {
                is StepProcessResult.Success -> {
                    println("✅ Payment step processed: ${paymentResult.message}")

                    // Check if payment was successful and trigger step rebuild
                    if (stepType == StepType.PAYMENT_CONFIRMATION) {
                        val paymentSuccessful =
                            accumulatedFormData["paymentSuccessful"]?.toBoolean() == true
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

            // ✅ Use stepType instead of checking field IDs
            when (stepData.stepType) {
                StepType.NAVIGATION_AREAS -> handleNavigationAreasSubmission(data)
                StepType.CREW_MANAGEMENT -> handleCrewSubmission(data)
                else -> {}
            }

            // If we just completed the Person Type step, and the selection was "فرد" (individual),
            // navigate to the dynamically-computed marine unit selection step instead of hardcoding indices.
            if (step == 0) {
                val incomingPersonType = data["selectionPersonType"]
                val currentPersonType =
                    incomingPersonType ?: accumulatedFormData["selectionPersonType"]
                if (currentPersonType == "فرد" || currentPersonType == "Individual") {
                    val stepsList = getSteps()
                    val marineStepIndex =
                        stepsList.indexOfFirst { it.titleRes == R.string.owned_ships }
                    return if (marineStepIndex >= 0) marineStepIndex else step + 1
                }
            } else if (step == 2 && data.filterValues { it == "[\"470123456\"]" }.isNotEmpty()) {
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
                        ?: throw com.informatique.mtcit.common.ApiException(
                            400,
                            if (AppLanguage.isArabic) "معرف السفينة غير موجود" else "Ship ID not found"
                        )

                    println("🔍 Extracted shipInfoId from formData: $shipInfoIdString")

                    // ✅ Clean the ship ID (remove array brackets if present)
                    when {
                        shipInfoIdString.startsWith("[\"") && shipInfoIdString.endsWith("\"]") -> {
                            // Array format: ["1674"] -> extract the number
                            shipInfoIdString.substring(2, shipInfoIdString.length - 2).toIntOrNull()
                                ?: throw com.informatique.mtcit.common.ApiException(
                                    400,
                                    if (AppLanguage.isArabic) "تنسيق معرف السفينة غير صحيح" else "Invalid ship ID format"
                                )
                        }

                        shipInfoIdString.startsWith("[") -> {
                            // Array format: ["1674"] -> extract the number
                            shipInfoIdString.trim('[', ']', '"').toIntOrNull()
                                ?: throw com.informatique.mtcit.common.ApiException(
                                    400,
                                    if (AppLanguage.isArabic) "تنسيق معرف السفينة غير صحيح" else "Invalid ship ID format"
                                )
                        }

                        else -> {
                            // Single value: "1674"
                            shipInfoIdString.toIntOrNull()
                                ?: throw com.informatique.mtcit.common.ApiException(
                                    400,
                                    if (AppLanguage.isArabic) "معرف السفينة غير صحيح" else "Invalid ship ID"
                                )
                        }
                    }

                    val requestId = navigationRequestId
                    if (requestId == null) {
                        throw Exception("No navigation request ID available. Ship selection might have failed.")
                    }

                    println("   Calling checkInspectionPreview with shipInfoId: $requestId")
                    val inspectionResult = marineUnitRepository.checkInspectionPreview(
                        requestId.toInt(),
                        transactionContext.inspectionPreviewBaseContext
                    )

                    // ✅ Handle inspection status - inspection-preview IS the send-request for navigation licenses
                    inspectionResult.fold(
                        onSuccess = { inspectionStatus ->
                            println("✅ Inspection preview check successful")
                            println("   Inspection status: $inspectionStatus (0=no inspection, 1=has inspection)")

                            if (inspectionStatus == 0) {
                                // ✅ Ship requires inspection - Show inspection dialog
                                println("⚠️ Ship requires inspection - showing inspection dialog")

                                // ✅ Show inspection required dialog with continue option and parent transaction info
                                // Request Type: 5 = Renew Navigation Permit
                                inspectionFlowManager.prepareInspectionDialog(
                                    message = if (AppLanguage.isArabic) "تم إرسال طلب تجديد تصريح الإبحار بنجاح (رقم الطلب: $requestId).\n\nالسفينة تحتاج إلى معاينة لإكمال الإجراءات. يرجى الاستمرار لتقديم طلب معاينة." else "Navigation permit renewal request submitted successfully (Request No: $requestId).\n\nThe ship requires an inspection. Please continue to submit an inspection request.",
                                    formData = accumulatedFormData,
                                    allowContinue = true,
                                    parentRequestId = requestId.toInt(),  // Convert Long to Int
                                    parentRequestType = 5  // Renew Navigation Permit
                                )

                                println("⚠️ Inspection required - blocking navigation to show dialog")
                                return -1 // ✅ Block navigation completely so dialog shows without proceeding

                            } else {
                                // ✅ Inspection done (data=1) - Show success dialog
                                println("✅ Ship has inspection completed - request submitted successfully")

                                // ✅ For navigation licenses, inspection-preview IS the send-request API
                                // No need to call separate send-request endpoint

                                val requestNumber = accumulatedFormData["requestSerial"]
                                    ?: accumulatedFormData["requestId"]
                                    ?: "N/A"

                                // ✅ NEW: Check if this is a NEW request (not resumed)
                                val isNewRequest =
                                    accumulatedFormData["isResumedTransaction"]?.toBoolean() != true

                                // ✅ Use hasAcceptance from strategy property (set from TransactionDetail API)
                                val strategyHasAcceptance = this.hasAcceptance

                                println("🔍 isNewRequest check:")
                                println("   - isResumedTransaction flag: ${accumulatedFormData["isResumedTransaction"]}")
                                println("   - isNewRequest result: $isNewRequest")
                                println("   - hasAcceptance (from strategy): $strategyHasAcceptance")

                                // ✅ Only stop if BOTH isNewRequest AND hasAcceptance are true
                                if (isNewRequest && strategyHasAcceptance) {
                                    println("🎉 NEW request submitted with hasAcceptance=true - showing success dialog and stopping")
                                    println("   User must continue from profile screen")

                                    // Set success flags for ViewModel to show dialog
                                    accumulatedFormData["requestSubmitted"] = "true"
                                    accumulatedFormData["requestNumber"] = requestNumber
                                    accumulatedFormData["successMessage"] = if (AppLanguage.isArabic) "تم إرسال الطلب بنجاح" else "Request submitted successfully"
                                    accumulatedFormData["needInspection"] = "false"

                                    // Return -2 to indicate: success but show dialog and stop
                                    return -2
                                } else if (isNewRequest && !strategyHasAcceptance) {
                                    println("✅ NEW request submitted with hasAcceptance=false - continuing to next steps")
                                    println("   Transaction will continue to payment/next steps")
                                    // Continue normally - don't return, let the flow proceed
                                } else {
                                    println("✅ Resumed request - continuing to payment")
                                    // Don't block - let payment steps show
                                    // Remove the return statement to allow payment flow
                                }
                            }
                        },
                        onFailure = { error ->
                            println("❌ Failed to check inspection preview: ${error.message}")
                            val msg = when (error) {
                                is ApiException -> error.message ?: if (AppLanguage.isArabic) "حدث خطأ أثناء التحقق من المعاينة" else "An error occurred while verifying the inspection"
                                else -> ErrorMessageExtractor.extract(error.message)
                            }
                            accumulatedFormData["apiError"] =
                                if (AppLanguage.isArabic) "حدث خطأ أثناء التحقق من المعاينة: $msg" else "An error occurred while verifying the inspection: $msg"
                            // Re-throw so ViewModel can show refresh button / error banner
                            if (error is ApiException) throw error
                            else throw ApiException(500, msg)
                        }
                    )

                    // ✅ Unreachable - kept for compilation
                } catch (e: Exception) {
                    println("❌ Exception in review step: ${e.message}")
                    e.printStackTrace()
                    accumulatedFormData["apiError"] =
                        if (AppLanguage.isArabic) "حدث خطأ أثناء إرسال الطلب: ${e.message}" else "An error occurred while submitting the request: ${e.message}"
                    return -1
                }
            }
        }

        return step
    }

    /**
     * Handle navigation areas submission (update existing or add new)
     * ✅ ALWAYS uses PUT API for renewal - whether user modified areas or not
     */
    private suspend fun handleNavigationAreasSubmission(data: Map<String, String>) {
        // ✅ Get selected names from form data - handle JSON array format
        val sailingRegionsString = data["sailingRegions"] ?: accumulatedFormData["sailingRegions"] ?: ""

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

        // ✅ Map names to IDs (match against localized name to be consistent with display)
        val selectedAreaIds = sailingRegionsOptions
            .filter { area -> selectedNames.contains(if (AppLanguage.isArabic) area.nameAr else area.nameEn) }
            .map { it.id }

        if (selectedAreaIds.isEmpty()) {
            println("⚠️ No navigation areas selected or no matching IDs found")
            println("⚠️ Selected names: $selectedNames")
            println("⚠️ Available regions: ${sailingRegionsOptions.map { it.nameAr }}")
            return
        }

        println("✅ Selected navigation areas: names=$selectedNames, ids=$selectedAreaIds")

        // ✅ Extract passengersNo from form data
        val passengersNoStr = data["passengersNo"] ?: accumulatedFormData["passengersNo"]
        val passengersNo = passengersNoStr?.trim()?.toIntOrNull()
        println("🔍 passengersNo: $passengersNo")

        // Ensure we have a request ID
        val requestId = navigationRequestId
        if (requestId == null) {
            println("❌ No requestId available - cannot update navigation areas")
            return
        }

        // ✅ ALWAYS use PUT API for renewal (update existing areas)
        // This is called whether user modified the areas or not
        println("🔄 Calling PUT /api/v1/navigation-license-renewal-request/$requestId/navigation-areas")
        println("   Sending areaIds: $selectedAreaIds, lastNavLicId: $lastNavLicId, passengersNo: $passengersNo")

        navigationLicenseManager.updateNavigationAreasRenew(
            requestId = requestId,
            areaIds = selectedAreaIds,
            lastNavLicId = lastNavLicId,
            passengersNo = passengersNo
        )
            .onSuccess {
                println("✅ Navigation areas updated successfully via PUT API")
            }
            .onFailure { error ->
                println("❌ Failed to update navigation areas: ${error.message}")
                val msg = when (error) {
                    is ApiException -> error.message ?: if (AppLanguage.isArabic) "فشل في تحديث مناطق الإبحار" else "Failed to update sailing areas"
                    else -> ErrorMessageExtractor.extract(error.message)
                }
                accumulatedFormData["apiError"] = msg
                lastApiError = msg
                if (error is ApiException) throw error else throw ApiException(500, msg)
            }
    }

    /**
     * Handle crew submission (manual or Excel)
     * Individual crew saves/deletes are now handled via immediate API calls from the UI.
     * This method only ensures the request exists and handles Excel upload if selected.
     */
    private suspend fun handleCrewSubmission(data: Map<String, String>) {
        // Ensure the renewal request has been created
        ensureRequestCreated() ?: return

        if (navigationLicenseManager.isExcelUploadSelected(data)) {
            // TODO: Handle Excel file upload path
            println("📤 Excel upload mode selected")
        } else {
            // Individual crew members are managed via immediate API calls triggered from the UI.
            // Nothing to bulk-submit here.
            println("ℹ️ Crew step - individual saves/deletes already sent to API immediately")
        }
    }

    // ========================================
    // IMMEDIATE CREW API CALLS
    // (called by MarineRegistrationViewModel for Renew Navigation Permit)
    // ========================================

    /**
     * Add a new crew member immediately via POST
     * Called when user saves a new sailor (apiId == null)
     */
    suspend fun addCrewMemberImmediate(sailor: SailorData): Result<com.informatique.mtcit.data.dto.CrewResDto> {
        val requestId = ensureRequestCreated()
            ?: return Result.failure(Exception(if (AppLanguage.isArabic) "فشل في الحصول على معرف الطلب" else "Failed to get request ID"))
        return navigationLicenseManager.addCrewMemberRenewImmediate(requestId, sailor)
    }

    /**
     * Update an existing crew member immediately via PUT
     * Called when user saves an existing sailor (apiId != null)
     */
    suspend fun updateCrewMemberImmediate(sailor: SailorData): Result<com.informatique.mtcit.data.dto.CrewResDto> {
        val requestId = ensureRequestCreated()
            ?: return Result.failure(Exception(if (AppLanguage.isArabic) "فشل في الحصول على معرف الطلب" else "Failed to get request ID"))
        val crewId = sailor.apiId
            ?: return Result.failure(Exception(if (AppLanguage.isArabic) "لا يوجد معرف API لفرد الطاقم" else "No API ID for crew member"))
        return navigationLicenseManager.updateCrewMemberRenewImmediate(requestId, crewId, sailor)
    }

    /**
     * Delete a crew member immediately via DELETE
     * Called when user deletes an existing sailor (apiId != null)
     */
    suspend fun deleteCrewMemberImmediate(sailor: SailorData): Result<Unit> {
        val requestId = ensureRequestCreated()
            ?: return Result.failure(Exception(if (AppLanguage.isArabic) "فشل في الحصول على معرف الطلب" else "Failed to get request ID"))
        val crewId = sailor.apiId
            ?: return Result.failure(Exception(if (AppLanguage.isArabic) "لا يوجد معرف API لفرد الطاقم" else "No API ID for crew member"))
        return navigationLicenseManager.deleteCrewMemberRenew(requestId, crewId)
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
                val msg = when (error) {
                    is ApiException -> error.message ?: if (AppLanguage.isArabic) "فشل في إنشاء طلب تجديد" else "Failed to create renewal request"
                    else -> ErrorMessageExtractor.extract(error.message)
                }
                accumulatedFormData["apiError"] = msg
                lastApiError = msg
                if (error is ApiException) throw error else throw ApiException(500, msg)
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
                "فرد", "Individual" -> {
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
            return FieldFocusResult.Error("companyRegistrationNumber", if (AppLanguage.isArabic) "رقم السجل التجاري مطلوب" else "Commercial registration number is required")
        }

        if (registrationNumber.length < 3) {
            return FieldFocusResult.Error("companyRegistrationNumber", if (AppLanguage.isArabic) "رقم السجل التجاري يجب أن يكون أكثر من 3 أرقام" else "Commercial registration number must be more than 3 digits")
        }

        return try {
            val result = companyRepository.fetchCompanyLookup(registrationNumber)
                .flowOn(Dispatchers.IO)
                .catch { throw Exception(if (AppLanguage.isArabic) "حدث خطأ أثناء البحث عن الشركة: ${it.message}" else "An error occurred while searching for the company: ${it.message}") }
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
                        FieldFocusResult.Error("companyRegistrationNumber", if (AppLanguage.isArabic) "لم يتم العثور على الشركة" else "Company not found")
                    }
                }
                is BusinessState.Error -> FieldFocusResult.Error("companyRegistrationNumber", result.message)
                is BusinessState.Loading -> FieldFocusResult.NoAction
            }
        } catch (e: Exception) {
            FieldFocusResult.Error("companyRegistrationNumber", e.message ?: if (AppLanguage.isArabic) "حدث خطأ غير متوقع" else "An unexpected error occurred")
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

    // ================================================================================
    // 🎯 DRAFT TRACKING: Extract completed steps from API response
    // ================================================================================
    override fun extractCompletedStepsFromApiResponse(response: Any): Set<StepType> {
        // TODO: Parse the renew navigation permit API response and determine which steps are completed
        val completedSteps = mutableSetOf<StepType>()

        println("⚠️ RenewNavigationPermitStrategy: extractCompletedStepsFromApiResponse not yet implemented")
        println("   Response type: ${response::class.simpleName}")

        return completedSteps
    }
}
