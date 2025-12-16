package com.informatique.mtcit.business.transactions

import android.content.Context
import com.informatique.mtcit.R
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.ui.components.PersonType
import com.informatique.mtcit.ui.components.SelectableItem
import com.informatique.mtcit.ui.viewmodels.StepData
import javax.inject.Inject
import com.informatique.mtcit.business.transactions.marineunit.rules.ReleaseMortgageRules
import com.informatique.mtcit.business.transactions.marineunit.usecases.ValidateMarineUnitUseCase
import com.informatique.mtcit.business.transactions.marineunit.usecases.GetEligibleMarineUnitsUseCase
import com.informatique.mtcit.data.repository.MarineUnitRepository
import com.informatique.mtcit.data.api.MortgageApiService
import com.informatique.mtcit.data.helpers.FileUploadHelper
import com.informatique.mtcit.data.model.OwnerFileUpload
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Strategy for Release Mortgage
 * Steps:
 * 1. Person Type Selection (Individual/Company)
 * 2. Commercial Registration (conditional - only for Company)
 * 3. Unit Selection (choose from mortgaged ships) - WITH BUSINESS VALIDATION
 * 4. Upload Documents (mortgage certificate)
 * 5. Review
 */
class ReleaseMortgageStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository,
    private val releaseMortgageRules: ReleaseMortgageRules,
    private val validateMarineUnitUseCase: ValidateMarineUnitUseCase,
    private val getEligibleUnitsUseCase: GetEligibleMarineUnitsUseCase,
    private val marineUnitRepository: MarineUnitRepository,
    private val mortgageApiService: MortgageApiService,
    @ApplicationContext private val appContext: Context
) : TransactionStrategy {

    private var portOptions: List<String> = emptyList()
    private var countryOptions: List<String> = emptyList()
    private var personTypeOptions: List<PersonType> = emptyList()
    private var commercialOptions: List<SelectableItem> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()
    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf()

    // ✅ NEW: Store required documents from API
    private var requiredDocuments: List<com.informatique.mtcit.data.model.RequiredDocumentItem> = emptyList()

    // ✅ Store the created redemption request ID for later status update
    private var createdRedemptionRequestId: Int? = null

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()
        val commercialRegistrations = lookupRepository.getCommercialRegistrations("12345678901234").getOrNull() ?: emptyList()

        println("📄 ReleaseMortgage - Fetching required documents from API...")
        val requestTypeId = TransactionType.RELEASE_MORTGAGE.toRequestTypeId()
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
        requiredDocuments = requiredDocumentsList // ✅ Store documents

        println("🚢 Skipping initial ship load - will load after user selects type and presses Next")

        return mapOf(
            "registrationPort" to ports,
            "ownerNationality" to countries,
            "ownerCountry" to countries,
            "bankCountry" to countries,
            "marineUnits" to emptyList<String>(),
            "commercialRegistration" to commercialRegistrations,
            "personType" to personTypes
        )
    }

    override suspend fun loadShipsForSelectedType(formData: Map<String, String>): List<MarineUnit> {
        val personType = formData["selectionPersonType"]
        // ✅ Extract CR Number from selectionData (not company name)
        val commercialReg = formData["selectionData"]

        println("🔒 loadShipsForSelectedType (RELEASE MORTGAGE) - personType=$personType, commercialReg=$commercialReg")

        // ✅ For individuals: send ownerCivilId + requestTypeId ONLY (no commercialNumber)
        // ✅ For companies: send ownerCivilId + requestTypeId + commercialNumber (CR Number)
        val (ownerCivilId, commercialRegNumber) = when (personType) {
            "فرد" -> {
                println("✅ Individual: Using ownerCivilId + requestTypeId ONLY")
                Pair("12345678", null) // TODO: Get from authenticated user
            }
            "شركة" -> {
                println("✅ Company: Using ownerCivilId + requestTypeId + commercialNumber (CR Number from selectionData)")
                Pair("12345678", commercialReg) // ✅ commercialReg contains CR Number (e.g., "123456-1")
            }
            else -> {
                println("⚠️ Unknown person type, using default (individual)")
                Pair("12345678", null)
            }
        }

        println("🔍 Calling loadShipsForOwner with:")
        println("   ownerCivilId=$ownerCivilId")
        println("   commercialRegNumber=$commercialRegNumber")
        println("   requestTypeId=13 (Release Mortgage)")

        // ✅ Use loadShipsForOwner instead of loadMortgagedShipsForOwner
        // This will filter ships by requestTypeId and send proper parameters based on person type
        marineUnits = marineUnitRepository.loadShipsForOwner(
            ownerCivilId = ownerCivilId,
            commercialRegNumber = commercialRegNumber, // ✅ null for individuals, CR Number for companies
            // **********************************************************************************************************
            //Request Type Id
            requestTypeId = TransactionType.RELEASE_MORTGAGE.toRequestTypeId() // ✅ Release Mortgage ID
        )
        println("✅ Loaded ${marineUnits.size} ships for Release Mortgage")
        return marineUnits
    }

    override suspend fun clearLoadedShips() {
        println("🧹 Clearing loaded ships cache")
        marineUnits = emptyList()
    }

    override fun updateAccumulatedData(data: Map<String, String>) {
        accumulatedFormData.putAll(data)
        println("📦 ReleaseMortgage - Updated accumulated data: $accumulatedFormData")
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
                allowMultipleSelection = releaseMortgageRules.allowMultipleSelection(),
                showOwnedUnitsWarning = true
            )
        )

        // Step 4: Upload Documents (Dynamic from API)
        println("🔍 DEBUG: requiredDocuments.size = ${requiredDocuments.size}")
        steps.add(
            SharedSteps.dynamicDocumentsStep(
                documents = requiredDocuments  // ✅ Pass documents from API
            )
        )

        // Step 5: Review
        steps.add(SharedSteps.reviewStep())

        return steps
    }

    // NEW: Validate marine unit selection with business rules
    suspend fun validateMarineUnitSelection(unitId: String, userId: String): ValidationResult {
        val unit = marineUnits.find { it.id.toString() == unitId }
            ?: return ValidationResult.Error("الوحدة البحرية غير موجودة")

        val (validationResult, navigationAction) = validateMarineUnitUseCase.executeAndGetAction(
            unit = unit,
            userId = userId,
            rules = releaseMortgageRules
        )

        return ValidationResult.Success(validationResult, navigationAction)
    }

    // NEW: Get only eligible units (mortgaged units) for this transaction
    suspend fun getEligibleMarineUnits(userId: String): List<MarineUnit> {
        return getEligibleUnitsUseCase.getEligibleOnly(userId, releaseMortgageRules)
    }

    override fun validateStep(step: Int, data: Map<String, Any>): Pair<Boolean, Map<String, String>> {
        val stepData = getSteps().getOrNull(step) ?: return Pair(false, emptyMap())
        val formData = data.mapValues { it.value.toString() }
        return validationUseCase.validateStep(stepData, formData)
    }

    override suspend fun processStepData(step: Int, data: Map<String, String>): Int {
        // ✅ Accumulate form data for dynamic step logic
        accumulatedFormData.putAll(data)
        println("📦 ReleaseMortgage - Accumulated data: $accumulatedFormData")

        // ✅ Check if this is the file upload step (step after marine unit selection)
        val steps = getSteps()
        val currentStepData = steps.getOrNull(step)

        // Check if the current step has the file upload field
        val hasFileUpload = currentStepData?.fields?.any { it.id == "ownershipProof" } == true

        if (hasFileUpload && accumulatedFormData.containsKey("ownershipProof")) {
            println("📤 File upload step completed - creating redemption request NOW")

            // Create the redemption request immediately
            try {
                val result = createRedemptionRequest(accumulatedFormData)

                result.onSuccess { response ->
                    // ✅ Store the request ID for later status update in review step
                    createdRedemptionRequestId = response.data.id
                    println("💾 STORED REDEMPTION REQUEST ID: $createdRedemptionRequestId")
                }

                result.onFailure { error ->
                    println("❌ Failed to create redemption request: ${error.message}")
                    println("🔄 Re-throwing error to prevent navigation and show error to user")
                    // ✅ Throw the error to prevent navigation and show error in UI
                    throw error
                }
            } catch (e: Exception) {
                println("❌ Exception in createRedemptionRequest: ${e.message}")
                e.printStackTrace()

                // ✅ Provide helpful error message in Arabic
                val userMessage = when {
                    e.message?.contains("400") == true ->
                        "خطأ في البيانات المرسلة إلى الخادم (400). يرجى التأكد من:\n" +
                        "• اختيار سفينة صحيحة\n" +
                        "• رفع ملف شهادة الرهن\n" +
                        "• الاتصال بالإنترنت"

                    e.message?.contains("404") == true ->
                        "لم يتم العثور على خدمة فك الرهن على الخادم (404)"

                    e.message?.contains("500") == true ->
                        "خطأ في الخادم (500). يرجى المحاولة لاحقاً"

                    e.message?.contains("timeout") == true || e.message?.contains("Timeout") == true ->
                        "انتهت مهلة الاتصال. يرجى التحقق من الإنترنت والمحاولة مجدداً"

                    else ->
                        "فشل إنشاء طلب فك الرهن: ${e.message}"
                }

                // Re-throw with user-friendly message
                throw Exception(userMessage)
            }
        }

        return step
    }

    /**
     * Create redemption request with the accumulated form data
     * This is called automatically after file upload step
     */
    private suspend fun createRedemptionRequest(formData: Map<String, String>): Result<com.informatique.mtcit.data.model.CreateMortgageRedemptionResponse> {
        println("=".repeat(80))
        println("🔓 Creating mortgage redemption request...")
        println("=".repeat(80))

        // Extract data from form
        val selectedUnitsJson = formData["selectedMarineUnits"]
        val ownershipProofUri = formData["ownershipProof"]

        println("📋 Form Data:")
        println("   Selected Units JSON: $selectedUnitsJson")
        println("   Ownership Proof URI: $ownershipProofUri")

        // Validate required fields
        if (selectedUnitsJson.isNullOrBlank() || selectedUnitsJson == "[]") {
            println("❌ Marine unit not selected")
            return Result.failure(Exception("يرجى اختيار السفينة"))
        }

        if (ownershipProofUri.isNullOrBlank() || !ownershipProofUri.startsWith("content://")) {
            println("❌ File not uploaded")
            return Result.failure(Exception("يرجى رفع ملف شهادة الرهن"))
        }

        // Parse the selected ship ID (similar to mortgage strategy)
        val shipId = try {
            val cleanJson = selectedUnitsJson.trim().removeSurrounding("[", "]")
            val maritimeIds = cleanJson.split(",").map { it.trim().removeSurrounding("\"") }
            val firstMaritimeId = maritimeIds.firstOrNull()

            if (firstMaritimeId.isNullOrBlank()) {
                println("❌ Failed to parse maritime ID from: $selectedUnitsJson")
                return Result.failure(Exception("تنسيق اختيار السفينة غير صالح"))
            }

            println("📍 Extracted maritime ID (MMSI): $firstMaritimeId")

            // Find the MarineUnit object that matches this maritimeId
            val selectedUnit = marineUnits.firstOrNull { it.maritimeId == firstMaritimeId }
            if (selectedUnit == null) {
                println("❌ Could not find MarineUnit with maritimeId: $firstMaritimeId")
                return Result.failure(Exception("السفينة المحددة غير موجودة"))
            }

            // Convert the actual ship ID to Int
            val actualShipId = selectedUnit.id.toIntOrNull()
            if (actualShipId == null) {
                println("❌ Ship ID is not a valid integer: ${selectedUnit.id}")
                return Result.failure(Exception("معرف السفينة غير صالح"))
            }

            println("✅ Found matching MarineUnit:")
            println("   Maritime ID (MMSI): ${selectedUnit.maritimeId}")
            println("   Actual Ship ID: $actualShipId")
            println("   Ship Name: ${selectedUnit.shipName}")

            actualShipId
        } catch (e: Exception) {
            println("❌ Exception parsing selected units: ${e.message}")
            e.printStackTrace()
            return Result.failure(Exception("فشل تحليل السفينة المحددة: ${e.message}"))
        }

        // Convert file URI to OwnerFileUpload
        val fileUpload = try {
            val uri = android.net.Uri.parse(ownershipProofUri)
            val engineFile = FileUploadHelper.uriToFileUpload(appContext, uri)
            if (engineFile == null) {
                println("❌ Could not convert URI to file upload")
                return Result.failure(Exception("فشل تحميل الملف"))
            }

            // Convert to OwnerFileUpload
            OwnerFileUpload(
                fileName = engineFile.fileName,
                fileUri = engineFile.fileUri,
                fileBytes = engineFile.fileBytes,
                mimeType = engineFile.mimeType ?: "application/octet-stream",
                docOwnerId = "ownershipProof",
                docId = 1
            )
        } catch (e: Exception) {
            println("❌ Exception converting file URI: ${e.message}")
            e.printStackTrace()
            return Result.failure(Exception("فشل معالجة الملف: ${e.message}"))
        }

        println("📎 File prepared: ${fileUpload.fileName} (${fileUpload.fileBytes.size} bytes)")

        // Create the redemption request
        val request = com.informatique.mtcit.data.model.CreateMortgageRedemptionRequest(
            shipInfoId = shipId,
            statusId = 1  // Always 1 for new requests
        )

        println("📤 Sending redemption request to API...")
        println("   Ship ID: ${request.shipInfoId}")
        println("   Status ID: ${request.statusId}")
        println("   File: ${fileUpload.fileName}")

        // Call API
        val result = mortgageApiService.createMortgageRedemptionRequest(request, listOf(fileUpload))

        result.onSuccess { response ->
            println("✅ Redemption request created successfully!")
            println("   Redemption ID: ${response.data.id}")
            println("   Ship ID: ${response.data.ship?.id}")
            println("   Status ID: ${response.data.status?.id}")
        }

        result.onFailure { error ->
            println("❌ Failed to create redemption request: ${error.message}")
        }

        println("=".repeat(80))

        return result
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        // ✅ Submit is not used for ReleaseMortgage - the request is created in processStepData
        // This is only here for interface compatibility
        println("⚠️ ReleaseMortgage.submit() called - but request was already created in processStepData")
        return Result.success(true)
    }

    // ✅ Implement interface methods for request ID and status update
    override fun getCreatedRequestId(): Int? {
        return createdRedemptionRequestId
    }

    override fun getStatusUpdateEndpoint(requestId: Int): String? {
        return "api/v1/mortgage-redemption-request/$requestId/update-status"
    }

    override fun getTransactionTypeName(): String {
        return "فك الرهن"
    }
}
