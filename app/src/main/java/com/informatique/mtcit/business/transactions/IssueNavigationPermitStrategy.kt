package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.R
import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.business.transactions.managers.NavigationLicenseManager
import com.informatique.mtcit.business.transactions.shared.StepType
import com.informatique.mtcit.business.transactions.shared.PaymentManager
import com.informatique.mtcit.common.ApiException  // ✅ Import ApiException for error handling
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
import com.informatique.mtcit.business.transactions.shared.ReviewManager
import com.informatique.mtcit.business.transactions.shared.StepProcessResult
import dagger.hilt.android.qualifiers.ApplicationContext
import com.informatique.mtcit.util.UserHelper
import io.ktor.utils.io.streams.asInput
import androidx.core.net.toUri

/**
 * Strategy for Issue Navigation Permit
 * Uses NavigationLicenseManager for all navigation license operations
 */
class IssueNavigationPermitStrategy @Inject constructor(
    private val companyRepository: CompanyRepo,
    private val validationUseCase: FormValidationUseCase,
    private val marineUnitRepository: MarineUnitRepository,
    private val lookupRepository: LookupRepository,
    private val navigationLicenseManager: NavigationLicenseManager,
    private val shipSelectionManager: com.informatique.mtcit.business.transactions.shared.ShipSelectionManager,
    private val paymentManager: PaymentManager,
    @ApplicationContext private val appContext: Context
 ) : BaseTransactionStrategy() {

    private var countryOptions: List<String> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()

    private var commercialOptions: List<SelectableItem> = emptyList()

    private var typeOptions: List<PersonType> = emptyList()
    private var sailingRegionsOptions: List<NavigationArea> = emptyList()
    private var crewJobTitles: List<String> = emptyList()

    private val requestTypeId = TransactionType.ISSUE_NAVIGATION_PERMIT.toRequestTypeId()
    private val transactionContext: TransactionContext = TransactionType.ISSUE_NAVIGATION_PERMIT.context
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

        // ✅ NEW: Payment Steps - Only show if we have requestId from review step
        val hasRequestId = accumulatedFormData["requestId"] != null

        if (hasRequestId) {
            // Payment Details Step - Shows payment breakdown
            steps.add(SharedSteps.paymentDetailsStep(accumulatedFormData))

            // Payment Success Step - Only show if payment was successful
            val paymentSuccessful = accumulatedFormData["paymentSuccessful"]?.toBoolean() == true
            if (paymentSuccessful) {
                steps.add(SharedSteps.paymentSuccessStep())
            }
        }

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
        if (stepData != null) {
            println("🔵 Processing Step ${step + 1}/${getSteps().size} - Type: ${stepData.stepType}, TitleRes: ${stepData.titleRes}")
            val stepType = stepData.stepType

            // ✅ Handle marine unit selection - call createIssueRequest after successful ship selection
            if (stepData.titleRes == R.string.owned_ships) {
                val isAddingNew = accumulatedFormData["isAddingNewUnit"]?.toBoolean() == true
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

                                // Extract and persist selected shipInfoId (clean first element)
                                val selectedUnits = selectedUnitsJson.let { sel ->
                                    try {
                                        val cleanJson = sel.trim().removeSurrounding("[", "]")
                                        val shipIds = cleanJson.split(",").map { it.trim().removeSurrounding("\"") }
                                        shipIds.firstOrNull()
                                    } catch (_: Exception) {
                                        null
                                    }
                                }

                                selectedUnits?.let { firstShipId ->
                                    accumulatedFormData["shipInfoId"] = firstShipId
                                    accumulatedFormData["coreShipsInfoId"] = firstShipId
                                    // ensureRequestCreated expects selectedMarineUnit (singular)
                                    accumulatedFormData["selectedMarineUnit"] = firstShipId
                                }
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
                                println("❌ Ship selection failed: ${selectionResult.message}")
                                accumulatedFormData["apiError"] = selectionResult.message
                                // ✅ Re-throw original exception if it's an ApiException to preserve error code (401, 406, etc.)
                                val originalException = selectionResult.originalException
                                if (originalException is ApiException) {
                                    println("🔄 Re-throwing original ApiException with code ${originalException.code}")
                                    throw originalException
                                } else {
                                    throw ApiException(500, selectionResult.message)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        accumulatedFormData["apiError"] = e.message ?: "فشل في متابعة الطلب"
                        return -1
                    }
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
                        val paymentSuccessful = accumulatedFormData["paymentSuccessful"]?.toBoolean() == true
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
                StepType.NAVIGATION_AREAS -> {
                    val hasError = handleNavigationAreasSubmission(data)
                    if (hasError) return -1 // ✅ Block navigation if error occurred
                }
                StepType.CREW_MANAGEMENT -> {
                    val hasError = handleCrewSubmission(data)
                    if (hasError) return -1 // ✅ Block navigation if error occurred
                }
                StepType.REVIEW -> {
                    println("📋 Handling Review Step using ReviewManager")

                    val requestIdInt = accumulatedFormData["requestId"]?.toIntOrNull()
                    if (requestIdInt == null) {
                        println("❌ No requestId available for review step")
                        accumulatedFormData["apiError"] = "لم يتم العثور على رقم الطلب"
                        return -1
                    }

                    try {
                        // ✅ STEP 1: Check inspection preview first
                        println("🔍 STEP 1: Checking inspection preview...")

                        // ✅ Use the navigationRequestId that was created after ship selection
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

                        // ✅ Code above handles both data=0 (inspection required) and data=1 (success)
                        // No additional code needed here - function returns early in both cases
                    } catch (e: Exception) {
                        println("❌ Exception in review step: ${e.message}")
                        e.printStackTrace()
                        accumulatedFormData["apiError"] =
                            "حدث خطأ أثناء إرسال الطلب: ${e.message}"
                        return -1
                    }
                }
                else -> {}
            }
        } else {
            println("🔵 Processing Step ${step + 1}/${getSteps().size} - Step data not found!")
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
            val simpleArrayRegex = """\["?(\d+)"?]""".toRegex()
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

        // Check if Excel file is uploaded
        val excelFileUri = data["crewExcelFile"]
        val sailorsJson = data["sailors"] ?: "[]"

        if (!excelFileUri.isNullOrBlank() && excelFileUri.startsWith("content://")) {
            // Excel file upload mode
            println("📤 Excel upload mode - file URI: $excelFileUri")

            try {
                // Convert URI to PartData
                val uri = excelFileUri.toUri()
                val contentResolver = appContext.contentResolver

                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw Exception("Cannot open file stream")

                val fileBytes = inputStream.readBytes()
                inputStream.close()

                // Get file name
                val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    cursor.getString(nameIndex)
                } ?: "crew.xlsx"

                println("📎 File: $fileName (${fileBytes.size} bytes)")

                // Create PartData for file upload
                val fileParts = listOf(
                    io.ktor.http.content.PartData.BinaryItem(
                        provider = { fileBytes.inputStream().asInput() },
                        dispose = {},
                        partHeaders = io.ktor.http.Headers.build {
                            append(
                                io.ktor.http.HttpHeaders.ContentDisposition,
                                "form-data; name=\"file\"; filename=\"$fileName\""
                            )
                            append(
                                io.ktor.http.HttpHeaders.ContentType,
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            )
                        }
                    )
                )

                // Call Excel upload API
                navigationLicenseManager.uploadCrewExcelIssue(requestId, fileParts).getOrThrow()

                println("✅ Successfully uploaded crew Excel file")
            } catch (e: Exception) {
                println("❌ Failed to upload Excel file: ${e.message}")
                throw Exception("فشل رفع ملف Excel: ${e.message}")
            }
        } else if (sailorsJson != "[]") {
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
        } else {
            println("⚠️ No crew data provided (neither Excel nor manual entry)")
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

    // ================================================================================
    // 🎯 DRAFT TRACKING: Extract completed steps from API response
    // ================================================================================
    override fun extractCompletedStepsFromApiResponse(response: Any): Set<StepType> {
        // TODO: Parse the navigation permit API response and determine which steps are completed
        val completedSteps = mutableSetOf<StepType>()

        println("⚠️ IssueNavigationPermitStrategy: extractCompletedStepsFromApiResponse not yet implemented")
        println("   Response type: ${response::class.simpleName}")

        return completedSteps
    }
}
