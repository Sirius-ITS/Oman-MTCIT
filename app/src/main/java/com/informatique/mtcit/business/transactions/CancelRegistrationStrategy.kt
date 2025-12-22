package com.informatique.mtcit.business.transactions

//Imports
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.informatique.mtcit.R
import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.PaymentManager
import com.informatique.mtcit.business.transactions.shared.ReviewManager
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.business.transactions.shared.StepType
import com.informatique.mtcit.data.model.cancelRegistration.DeletionFileUpload
import com.informatique.mtcit.data.model.cancelRegistration.DeletionSubmitResponse
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
 * Strategy for Cancel Permanent Registration (Deletion/Removal)
 * DEMONSTRATION: Highly simplified - minimal fields, just cancellation info
 * Shows extreme case of field removal for streamlined process
 */
class CancelRegistrationStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val companyRepository: CompanyRepo,
    private val validationUseCase: FormValidationUseCase,
    private val marineUnitRepository: MarineUnitRepository,
    private val lookupRepository: LookupRepository,
    private val marineUnitsApiService: com.informatique.mtcit.data.api.MarineUnitsApiService,
    private val shipSelectionManager: com.informatique.mtcit.business.transactions.shared.ShipSelectionManager,
    private val reviewManager: ReviewManager,
    private val paymentManager: PaymentManager,
    @ApplicationContext private val appContext: Context  // ✅ Injected context
) : TransactionStrategy {

    // ✅ Transaction context with all API endpoints
    private val transactionContext: TransactionContext = TransactionType.CANCEL_PERMANENT_REGISTRATION.context
    private val requestTypeId = TransactionType.CANCEL_PERMANENT_REGISTRATION.toRequestTypeId()
    private var portOptions: List<String> = emptyList()
    private var countryOptions: List<String> = emptyList()
    private var shipTypeOptions: List<String> = emptyList()
    private var commercialOptions: List<SelectableItem> = emptyList()
    private var typeOptions: List<PersonType> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()
    private var deletionReasonOptions: List<String> = emptyList() // ✅ NEW: Dynamic deletion reasons
    private var deletionReasonMap: Map<String, Int> = emptyMap() // ✅ NEW: Map name to ID
    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf() // ✅ Track form data// Store API error to prevent navigation and show error dialog
    private var lastApiError: String? = null
    private var requiredDocuments: List<com.informatique.mtcit.data.model.RequiredDocumentItem> = emptyList() // ✅ NEW: Store required documents from API
    private var deletionRequestId: Int? = null // ✅ NEW: Store created deletion request ID

    var context: android.content.Context? = null // ✅ Store Android context reference (will be set by ViewModel)
        set(value) {
            field = value
            androidContext = value
            println("✅ Context set in CancelRegistrationStrategy")
        }

    private var androidContext: android.content.Context? = null // ✅ Store Android context reference

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        // Load all dropdown options from API
        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val shipTypes = lookupRepository.getShipTypes().getOrNull() ?: emptyList()
        val commercialRegistrations = lookupRepository.getCommercialRegistrations("12345678901234").getOrNull() ?: emptyList()
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()

        // ✅ NEW: Fetch deletion reasons from API
        try {
            val deletionReasonsResult = repository.getDeletionReasons()
            deletionReasonsResult.onSuccess { reasonsResponse ->
                // Create map of name -> id for later lookup
                val reasonsList = reasonsResponse.data?.content?.mapNotNull { item ->
                    val name = item?.nameAr
                    val id = item?.id
                    if (name != null && id != null) {
                        println("🗑️ Loaded reason: '$name' -> ID: $id")
                        Pair(name, id)
                    } else {
                        println("⚠️ Skipping reason with null name or id: $item")
                        null
                    }
                } ?: emptyList()

                deletionReasonOptions = reasonsList.map { it.first }
                deletionReasonMap = reasonsList.toMap()

                println("🗑️ ========== Deletion Reasons Loaded ==========")
                println("🗑️ Total reasons: ${deletionReasonOptions.size}")
                println("🗑️ Deletion Reason Options: $deletionReasonOptions")
                println("🗑️ Deletion Reason Map: $deletionReasonMap")
                println("🗑️ ============================================")
            }.onFailure { error ->
                println("❌ Error fetching deletion reasons: ${error.message}")
                error.printStackTrace()
            }
        } catch (e: Exception) {
            println("❌ Exception fetching deletion reasons: ${e.message}")
            e.printStackTrace()
        }

        // ✅ Fetch required documents من الـ API
        println("📄 CancelRegistration - Fetching required documents from API...")
        val requiredDocumentsList = lookupRepository.getRequiredDocumentsByRequestType(requestTypeId)
            .getOrElse { error ->
                println("❌ ERROR fetching required documents: ${error.message}")
                error.printStackTrace()
                emptyList()
            }

        println("✅ Fetched ${requiredDocumentsList.size} required documents:")
        requiredDocumentsList.forEach { docItem ->
            val mandatoryText = if (docItem.document.isMandatory == 1) "إلزامي" else "اختياري"
            println("   - ${docItem.document.nameAr} ($mandatoryText)")
        }

        // Cache the options for use in getSteps()
        portOptions = ports
        countryOptions = countries
        shipTypeOptions = shipTypes
        commercialOptions = commercialRegistrations
        typeOptions = personTypes
        requiredDocuments = requiredDocumentsList // ✅ Store documents for later use

        println("🚢 Skipping initial ship load - will load after user selects type and presses Next")

        return mapOf(
            "marineUnits" to emptyList<MarineUnit>(),
            "registrationPort" to ports,
            "ownerNationality" to countries,
            "ownerCountry" to countries,
            "registrationCountry" to countries,
            "unitType" to shipTypes,
            "deletionReasons" to deletionReasonOptions, // ✅ NEW: Return deletion reasons
            "requiredDocuments" to requiredDocumentsList
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
            requestTypeId = requestTypeId
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
        println("📦 CancelRegistration - Updated accumulated data: $accumulatedFormData")
    }

    override fun getContext(): TransactionContext {
        TODO("Not yet implemented")
    }

    override fun getSteps(): List<StepData> {
        val steps = mutableListOf<StepData>()

        // Step 1: Person Type
        steps.add(SharedSteps.personTypeStep(typeOptions))

        // Step 2: Commercial Registration (only for companies)
        val selectedPersonType = accumulatedFormData["selectionPersonType"]
        if (selectedPersonType == "شركة") {
            steps.add(SharedSteps.commercialRegistrationStep(commercialOptions))
        }

        // Step 3: Marine Unit Selection
        steps.add(
            SharedSteps.marineUnitSelectionStep(
                units = marineUnits,
                allowMultipleSelection = false,
                showOwnedUnitsWarning = true
            )
        )

        // OLD Step 4: Cancellation Reason
        /*steps.add(
            StepData(
                titleRes = R.string.cancellation_reason,
                descriptionRes = R.string.cancellation_reason_desc,
                fields = listOf(
                    FormField.DropDown(
                        id = "cancellationReason",
                        labelRes = R.string.reason_for_cancellation,
                        mandatory = true,
                        options = deletionReasonOptions // ✅ Use dynamic options from API
                    ),
                    FormField.FileUpload(
                        id = "reasonProofDocument",
                        labelRes = R.string.reason_proof_document,
                        allowedTypes = listOf("pdf", "jpg", "jpeg", "png", "doc", "docx"),
                        maxSizeMB = 5,
                        mandatory = true
                    )
                )
            )
        )*/

        // ✅ Step 4: Cancellation Reason + Documents (dynamic)
        steps.add(
            SharedSteps.createCancellationReasonStep(
                deletionReasons = deletionReasonOptions,
                requiredDocuments = requiredDocuments
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

    override fun validateStep(step: Int, data: Map<String, Any>): Pair<Boolean, Map<String, String>> {
        val stepData = getSteps().getOrNull(step) ?: return Pair(false, emptyMap())
        val formData = data.mapValues { it.value.toString() }
        return validationUseCase.validateStep(stepData, formData)
    }

    override suspend fun processStepData(step: Int, data: Map<String, String>): Int {
        println("📄 processStepData called with: $data")

        // ✅ Accumulate form data
        accumulatedFormData.putAll(data)
        println("📦 CancelRegistration - Accumulated data: $accumulatedFormData")

        // Clear previous error
        lastApiError = null

        // Check current step
        val currentSteps = getSteps()
        val currentStepData = currentSteps.getOrNull(step)
        val stepType = currentStepData?.stepType

        println("🔍 Current step titleRes: ${currentStepData?.titleRes}")
        println("🔍 Current step type: $stepType")

        // ✅ NEW: Check if we just completed the Marine Unit Selection step
        if (currentStepData?.titleRes == R.string.owned_ships) {
            println("🚢 ✅ Marine Unit Selection step completed - using ShipSelectionManager...")

            // ✅ Use ShipSelectionManager
            val result = shipSelectionManager.handleShipSelection(
                shipId = data["selectedMarineUnits"],
                context = transactionContext
            )

            when (result) {
                is com.informatique.mtcit.business.transactions.shared.ShipSelectionResult.Success -> {
                    println("✅ Ship selection successful!")
                    deletionRequestId = result.requestId
                    accumulatedFormData["requestId"] = result.requestId.toString()
                    accumulatedFormData["createdRequestId"] = result.requestId.toString()

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
                }
                is com.informatique.mtcit.business.transactions.shared.ShipSelectionResult.Error -> {
                    println("❌ Ship selection failed: ${result.message}")
                    lastApiError = result.message
                    return -1
                }
            }
        }

        // ✅ The cancellation reason step has titleRes = R.string.cancellation_reason
        if (currentStepData?.titleRes == R.string.cancellation_reason) {
            println("🗑️ ✅ Cancellation Reason step completed - calling API...")

            var apiCallSucceeded = false
            try {
                // ✅ Collect all uploaded documents from dynamic fields
                val uploadedDocuments = collectUploadedDocuments(accumulatedFormData)

                println("📋 Total documents to upload: ${uploadedDocuments.size}")

                // ✅ Call the API with documents
                val result = submitDeletionWithFiles(accumulatedFormData, uploadedDocuments)

                result.fold(
                    onSuccess = { response ->
                        println("✅ Deletion request created successfully!")
                        println("   Deletion Request ID: ${response.data?.id}")
                        println("   Response: ${response.message}")

                        // ✅ CRITICAL: Store the deletion request ID in the member variable
                        deletionRequestId = response.data?.id
                        println("💾 STORED deletionRequestId = $deletionRequestId")

                        // Store success flag
                        accumulatedFormData["submissionSuccess"] = "true"
                        lastApiError = null
                        apiCallSucceeded = true
                    },
                    onFailure = { error ->
                        println("❌ Failed to create deletion request: ${error.message}")
                        error.printStackTrace()

                        // Store error for display
                        lastApiError = error.message ?: "حدث خطأ أثناء إنشاء طلب الشطب"
                        apiCallSucceeded = false
                    }
                )
            } catch (e: Exception) {
                println("❌ Exception while creating deletion request: ${e.message}")
                e.printStackTrace()

                lastApiError = e.message ?: "حدث خطأ غير متوقع"
                apiCallSucceeded = false
            }

            // ✅ Return -1 to prevent navigation if API call failed
            if (!apiCallSucceeded) {
                println("⚠️ API call failed - returning -1 to prevent navigation")
                return -1
            }
        }

        // ✅ NEW: Handle REVIEW step using ReviewManager
        if (stepType == StepType.REVIEW) {
            println("📋 Handling Review Step using ReviewManager")

            val requestIdInt = accumulatedFormData["requestId"]?.toIntOrNull()
            if (requestIdInt == null) {
                println("❌ No requestId available for review step")
                lastApiError = "لم يتم العثور على رقم الطلب"
                return -1
            }

            try {
                val endpoint = transactionContext.sendRequestEndpoint
                val contextName = transactionContext.displayName

                println("🚀 Calling ReviewManager.processReviewStep:")
                println("   Endpoint: $endpoint")
                println("   RequestId: $requestIdInt")
                println("   Context: $contextName")

                val result = reviewManager.processReviewStep(
                    endpoint = endpoint,
                    requestId = requestIdInt,
                    transactionName = contextName
                )

                when (result) {
                    is com.informatique.mtcit.business.transactions.shared.ReviewResult.Success -> {
                        println("✅ Review step processed successfully!")
                        println("   Message: ${result.message}")
                        println("   Need Inspection: ${result.needInspection}")

                        // Store response in formData
                        accumulatedFormData["needInspection"] = result.needInspection.toString()
                        accumulatedFormData["sendRequestMessage"] = result.message

                        // Check if inspection is required
                        if (result.needInspection) {
                            println("🔍 Inspection required for this request")
                            accumulatedFormData["showInspectionDialog"] = "true"
                            accumulatedFormData["inspectionMessage"] = result.message
                            return step
                        }
                    }
                    is com.informatique.mtcit.business.transactions.shared.ReviewResult.Error -> {
                        println("❌ Review step failed: ${result.message}")
                        lastApiError = result.message
                        return -1
                    }
                }
            } catch (e: Exception) {
                println("❌ Exception in review step: ${e.message}")
                e.printStackTrace()
                lastApiError = e.message ?: "حدث خطأ أثناء مراجعة الطلب"
                return -1
            }
        }

        // ✅ NEW: Handle PAYMENT step using PaymentManager
        if (stepType == StepType.PAYMENT) {
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

    // ✅ NEW: Collect uploaded documents from form data
    private suspend fun collectUploadedDocuments(
        formData: Map<String, String>
    ): List<DeletionFileUpload> {
        val uploadedDocuments = mutableListOf<DeletionFileUpload>()

        // ✅ Use the context set by ViewModel (not appContext)
        val ctx = androidContext ?: context ?: appContext

        if (ctx == null) {
            println("❌ CRITICAL: No context available!")
            return emptyList()
        }

        println("✅ Using context: ${ctx.javaClass.simpleName}")

        // Get all document fields (document_43, document_44, etc.)
        requiredDocuments
            .filter { it.document.isActive == 1 }
            .forEach { docItem ->
                val fieldId = "document_${docItem.document.id}"
                val documentUri = formData[fieldId]

                if (!documentUri.isNullOrBlank() && documentUri.startsWith("content://")) {
                    try {
                        val uri = android.net.Uri.parse(documentUri)

                        val bytes = ctx.contentResolver.openInputStream(uri)?.use {
                            it.readBytes()
                        } ?: throw Exception("Unable to read file")

                        val fileName = getFileNameFromUri(ctx, uri)
                            ?: "document_${docItem.document.id}_${System.currentTimeMillis()}"

                        val mimeType = ctx.contentResolver.getType(uri)
                            ?: "application/octet-stream"

                        val deletionFile = DeletionFileUpload(
                            fileName = fileName,
                            fileBytes = bytes,
                            mimeType = mimeType,
                            fileUri = uri.toString(),
                            docOwnerId = "document_${docItem.document.id}",
                            docId = docItem.document.id
                        )

                        uploadedDocuments.add(deletionFile)
                        println("📎 Added document: ${docItem.document.nameAr} (id=${docItem.document.id}, file=$fileName, size=${bytes.size} bytes)")

                    } catch (e: Exception) {
                        println("⚠️ Failed to process document ${docItem.document.nameAr}: ${e.message}")
                        e.printStackTrace()
                    }
                } else {
                    println("⚠️ Skipping document ${docItem.document.nameAr}: invalid URI '$documentUri'")
                }
            }

        return uploadedDocuments
    }

    // Helper function to get filename from URI
    private fun getFileNameFromUri(context: Context, uri: android.net.Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun submitDeletionWithFiles(
        data: Map<String, String>,
        files: List<DeletionFileUpload>
    ): Result<DeletionSubmitResponse> {
        return try {
            println("📤 ========== submitDeletionWithFiles ==========")
            println("📤 Form data keys: ${data.keys}")

            // Extract deletion reason ID
            val deletionReasonName = data["cancellationReason"]
                ?: return Result.failure(Exception("سبب الشطب مطلوب"))

            val deletionReasonId = deletionReasonMap[deletionReasonName]
                ?: return Result.failure(Exception("سبب الشطب غير صحيح"))

            // ✅ FIX: Extract ship info ID from correct field
            // The marine unit selection field stores data in "selectedMarineUnits" (JSON array)
            val selectedUnitsJson = data["selectedMarineUnits"]
                ?: return Result.failure(Exception("السفينة مطلوبة"))

            println("🔍 Selected units JSON: $selectedUnitsJson")

            // Parse the JSON array to get the first marine unit ID
            val shipInfoId = try {
                // Remove brackets and quotes, split by comma, take first
                val cleanJson = selectedUnitsJson.trim().removeSurrounding("[", "]")
                val shipIds = cleanJson.split(",").map { it.trim().removeSurrounding("\"") }
                val firstShipId = shipIds.firstOrNull()

                if (firstShipId.isNullOrBlank()) {
                    println("❌ Failed to parse maritime ID from: $selectedUnitsJson")
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

            println("📤 Files received: ${files.size}")

            files.forEachIndexed { index, file ->
                println("📎 File $index: ${file.fileName} (${file.fileBytes.size} bytes)")
            }

            if (files.isEmpty()) {
                println("❌ ERROR: No files provided!")
                return Result.failure(Exception("يجب إرفاق مستند واحد على الأقل"))
            }

            println("📤 Submitting: reasonId=$deletionReasonId, shipId=$shipInfoId, files=${files.size}")

            // ✅ Call repository
            val result = repository.submitDeletionRequest(deletionReasonId, shipInfoId, files)

            result
        } catch (e: Exception) {
            println("❌ Exception: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        return repository.submitRegistration(data)
    }

    override fun handleFieldChange(fieldId: String, value: String, formData: Map<String, String>): Map<String, String> {
        // Handle owner type change
        if (fieldId == "owner_type") {
            val mutableFormData = formData.toMutableMap()
            when (value) {
                "فرد" -> {
                    mutableFormData.remove("companyName")
                    mutableFormData.remove("companyRegistrationNumber")
                }
                "شركة" -> {
                    // Company fields will be shown and are required
                }
                "شراكة" -> {
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
            return FieldFocusResult.Error(
                "companyRegistrationNumber",
                "رقم السجل التجاري يجب أن يكون أكثر من 3 أرقام"
            )
        }

        return try {
            val result = companyRepository.fetchCompanyLookup(registrationNumber)
                .flowOn(Dispatchers.IO)
                .catch { throwable ->
                    throw Exception("حدث خطأ أثناء البحث عن الشركة: ${throwable.message}")
                }
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
                is BusinessState.Error -> {
                    FieldFocusResult.Error("companyRegistrationNumber", result.message)
                }
                is BusinessState.Loading -> {
                    FieldFocusResult.NoAction
                }
            }
        } catch (e: Exception) {
            FieldFocusResult.Error("companyRegistrationNumber", e.message ?: "حدث خطأ غير متوقع")
        }
    }

    fun getDeletionRequestId(): Int? {
        println("🔍 getDeletionRequestId called")
        println("   deletionRequestId = $deletionRequestId")
        println("   accumulatedFormData['deletionRequestId'] = ${accumulatedFormData["deletionRequestId"]}")

        // ✅ Fallback: Try to get from accumulated form data if member variable is null
        if (deletionRequestId == null) {
            val idFromFormData = accumulatedFormData["deletionRequestId"]?.toIntOrNull()
            if (idFromFormData != null) {
                println("⚠️ deletionRequestId was null, using value from formData: $idFromFormData")
                deletionRequestId = idFromFormData
            }
        }

        return deletionRequestId
    }

}

