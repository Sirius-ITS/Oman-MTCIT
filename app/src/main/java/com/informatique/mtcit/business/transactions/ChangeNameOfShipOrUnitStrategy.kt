package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.transactions.marineunit.rules.TemporaryRegistrationRules
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.validation.rules.ValidationRule
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.data.repository.MarineUnitRepository
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.ui.components.PersonType
import com.informatique.mtcit.ui.components.SelectableItem
import com.informatique.mtcit.ui.repo.CompanyRepo
import com.informatique.mtcit.ui.viewmodels.StepData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import javax.inject.Inject
import com.informatique.mtcit.ui.components.EngineData as UIEngineData
import com.informatique.mtcit.ui.components.OwnerData as UIOwnerData
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.informatique.mtcit.business.transactions.shared.Certificate
import com.informatique.mtcit.business.transactions.shared.StepType
import com.informatique.mtcit.data.repository.CertificateLocalData
import dagger.hilt.android.qualifiers.ApplicationContext
import com.informatique.mtcit.util.UserHelper

class ChangeNameOfShipOrUnitStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val companyRepository: CompanyRepo,
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository,
    private val marineUnitRepository: MarineUnitRepository,
    private val temporaryRegistrationRules: TemporaryRegistrationRules,
    @ApplicationContext private val appContext: Context
) : BaseTransactionStrategy() {
    private val transactionContext: TransactionContext = TransactionType.SHIP_NAME_CHANGE.context
    private var portOptions: List<String> = emptyList()
    private var countryOptions: List<String> = emptyList()
    private var shipTypeOptions: List<String> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()
    private var commercialOptions: List<SelectableItem> = emptyList()
    private var typeOptions: List<PersonType> = emptyList()
    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf()
    private var isFishingBoat: Boolean = false // ✅ Track if selected type is fishing boat
    private var fishingBoatDataLoaded: Boolean = false // ✅ Track if data loaded from Ministry
    private var loadedCertificates =  (mutableStateListOf<Certificate>())

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        // ✅ Get civilId from token
        val ownerCivilId = UserHelper.getOwnerCivilId(appContext)
        println("🔑 Owner CivilId from token: $ownerCivilId")

        loadedCertificates.addAll(CertificateLocalData.getSampleCertificates())
        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val shipTypes = lookupRepository.getShipTypes().getOrNull() ?: emptyList()

        // ✅ Handle null civilId - return empty list if no token
        val commercialRegistrations = if (ownerCivilId != null) {
            lookupRepository.getCommercialRegistrations(ownerCivilId).getOrNull() ?: emptyList()
        } else {
            emptyList()
        }
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()

        portOptions = ports
        countryOptions = countries
        shipTypeOptions = shipTypes
        commercialOptions = commercialRegistrations
        typeOptions = personTypes

        // ✅ Don't load ships here - they will be loaded when user presses Next
        // after selecting person type (individual/company)
        println("🚢 Skipping initial ship load - will load after user selects type and presses Next")

        return mapOf(
            "marineUnits" to emptyList<MarineUnit>(), // ✅ Empty initially
            "registrationPort" to ports,
            "ownerNationality" to countries,
            "ownerCountry" to countries,
            "registrationCountry" to countries,
            "unitType" to shipTypes,
            "commercialRegistration" to commercialRegistrations,
            "personType" to personTypes,
            "certificates" to loadedCertificates
        )
    }

    /**
     * ✅ NEW: Load ships when user selects type and presses Next
     */
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
            requestTypeId = "14" // ✅ Ship Name Change ID
        )

        println("✅ Loaded ${marineUnits.size} ships")
        marineUnits.forEach { unit ->
            println("   - ${unit.shipName} (ID: ${unit.id})")
        }

        return marineUnits
    }

    /**
     * ✅ NEW: Clear loaded ships when user goes back
     */
    override suspend fun clearLoadedShips() {
        println("🧹 Clearing loaded ships cache")
        marineUnits = emptyList()
    }

    override fun updateAccumulatedData(data: Map<String, String>) {
        accumulatedFormData.putAll(data)
        println("📦 TemporaryRegistration - Updated accumulated data: $accumulatedFormData")
    }

    override fun getContext(): TransactionContext {
        TODO("Not yet implemented")
    }

    override fun getSteps(): List<StepData> {
        val steps = mutableListOf<StepData>()

        // Step 1: Person Type
        steps.add(SharedSteps.personTypeStep(typeOptions))

        // Step 2: Commercial Registration (فقط للشركات)
        val selectedPersonType = accumulatedFormData["selectionPersonType"]
        if (selectedPersonType == "شر��ة") {
            steps.add(SharedSteps.commercialRegistrationStep(commercialOptions))
        }

        // Step 3: Marine Unit Selection ( commented for now )
        /*steps.add(
            SharedSteps.marineUnitSelectionStep(
                units = marineUnits,
                allowMultipleSelection = false,
                showAddNewButton = true,
                showOwnedUnitsWarning = true
            )
        )*/

        // Step 4: Marine Unit Name Selection
        steps.add(
            SharedSteps.marineUnitNameSelectionStep(
                showReservationInfo = true
            )
        )

        // Step 5: Shared Certificate Step
        steps.add(SharedSteps.createCertificateStep(loadedCertificates))

        println("📋 Total steps count: ${steps.size}")
        return steps
    }

    override fun validateStep(step: Int, data: Map<String, Any>): Pair<Boolean, Map<String, String>> {
        val stepData = getSteps().getOrNull(step) ?: return Pair(false, emptyMap())
        val formData = data.mapValues { it.value.toString() }

        // ✅ Get validation rules for this step
        val rules = getValidationRulesForStep(step, stepData)

        // ✅ Use accumulated data for validation (enables cross-step validation)
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

        return rules
    }

    override suspend fun processStepData(step: Int, data: Map<String, String>): Int {
        println("🔄 processStepData called with: $data")

        // ✅ Update accumulated data
        accumulatedFormData.putAll(data)

        println("📦 accumulatedFormData after update: $accumulatedFormData")

        // ... rest of existing code

        return step
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        return repository.submitRegistration(data)
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

        // ✅ Handle fishing boat selection from unitType dropdown
        if (fieldId == "unitType") {
            println("🔍 DEBUG - unitType changed to: $value")

            // Check if the selected type is fishing boat
            if (value == "قارب صيد" || value.contains("صيد") || value.contains("Fishing")) {
                println("✅ Fishing boat selected! Setting flag and storing in accumulated data")
                isFishingBoat = true
                fishingBoatDataLoaded = false // Reset loaded flag when type changes
                accumulatedFormData["isFishingBoat"] = "true"
                // ✅ Store the unitType mortgageValue immediately
                accumulatedFormData["unitType"] = value
            } else {
                println("❌ Not a fishing boat. Hiding agriculture field")
                isFishingBoat = false
                fishingBoatDataLoaded = false
                accumulatedFormData.remove("isFishingBoat")
                accumulatedFormData.remove("agricultureRequestNumber")
                // ✅ Store the unitType mortgageValue immediately
                accumulatedFormData["unitType"] = value
            }

            // ✅ Return updated formData to trigger step refresh
            val updatedFormData = formData.toMutableMap()
            updatedFormData["unitType"] = value
            updatedFormData["_triggerRefresh"] = System.currentTimeMillis().toString()
            return updatedFormData
        }

        return formData
    }

    override suspend fun onFieldFocusLost(fieldId: String, value: String): FieldFocusResult {
        if (fieldId == "companyRegistrationNumber") {
            return handleCompanyRegistrationLookup(value)
        }

        // ✅ Handle agriculture request number lookup for fishing boats
        if (fieldId == "agricultureRequestNumber") {
            return handleAgricultureRequestLookup(value)
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
     * Handle Ministry of Agriculture request number lookup for fishing boats
     * Fetches all boat data from Ministry API and auto-fills form fields
     */
    private suspend fun handleAgricultureRequestLookup(requestNumber: String): FieldFocusResult {
        if (requestNumber.isBlank()) {
            return FieldFocusResult.Error("agricultureRequestNumber", "رقم طلب وزارة الزراعة مطلوب")
        }

        if (requestNumber.length < 5) {
            return FieldFocusResult.Error("agricultureRequestNumber", "رقم الطلب يجب أن يكون 5 أرقام على الأقل")
        }

        return try {
            println("🔍 Fetching fishing boat data from Ministry of Agriculture...")

            // ✅ Use marineUnitRepository instead of agricultureRepository
            val result = marineUnitRepository.getFishingBoatData(requestNumber)

            if (result.isSuccess) {
                val boatData = result.getOrNull()

                if (boatData != null) {
                    println("✅ Boat data loaded successfully from Ministry")

                    // ✅ Mark that data has been loaded
                    fishingBoatDataLoaded = true
                    accumulatedFormData["fishingBoatDataLoaded"] = "true"

                    // ✅ Auto-fill ALL form fields with data from Ministry
                    val fieldsToUpdate = mutableMapOf<String, String>()

                    // Unit Selection Data
                    fieldsToUpdate["unitType"] = boatData.unitType
                    fieldsToUpdate["unitClassification"] = boatData.unitClassification
                    fieldsToUpdate["callSign"] = boatData.callSign
                    boatData.imoNumber?.let { fieldsToUpdate["imoNumber"] = it }
                    fieldsToUpdate["registrationPort"] = boatData.registrationPort
                    boatData.mmsi?.let { fieldsToUpdate["mmsi"] = it }
                    fieldsToUpdate["manufacturerYear"] = boatData.manufacturerYear
                    fieldsToUpdate["maritimeActivity"] = boatData.maritimeActivity
                    boatData.buildingDock?.let { fieldsToUpdate["buildingDock"] = it }
                    boatData.constructionPool?.let { fieldsToUpdate["constructionPool"] = it }
                    boatData.buildingMaterial?.let { fieldsToUpdate["buildingMaterial"] = it }
                    boatData.constructionStartDate?.let { fieldsToUpdate["constructionStartDate"] = it }
                    boatData.constructionEndDate?.let { fieldsToUpdate["constructionEndDate"] = it }
                    boatData.buildingCountry?.let { fieldsToUpdate["buildingCountry"] = it }
                    boatData.firstRegistrationDate?.let { fieldsToUpdate["registrationDate"] = it }
                    boatData.registrationCountry?.let { fieldsToUpdate["registrationCountry"] = it }

                    // Dimensions
                    fieldsToUpdate["overallLength"] = boatData.overallLength
                    fieldsToUpdate["overallWidth"] = boatData.overallWidth
                    fieldsToUpdate["depth"] = boatData.depth
                    boatData.height?.let { fieldsToUpdate["height"] = it }
                    boatData.decksCount?.let { fieldsToUpdate["decksCount"] = it }

                    // Weights
                    fieldsToUpdate["grossTonnage"] = boatData.grossTonnage
                    fieldsToUpdate["netTonnage"] = boatData.netTonnage
                    boatData.staticLoad?.let { fieldsToUpdate["staticLoad"] = it }
                    boatData.maxPermittedLoad?.let { fieldsToUpdate["maxPermittedLoad"] = it }

                    // Owner Info (Primary Owner - for backward compatibility)
                    fieldsToUpdate["ownerFullNameAr"] = boatData.ownerFullNameAr
                    boatData.ownerFullNameEn?.let { fieldsToUpdate["ownerFullNameEn"] = it }
                    fieldsToUpdate["ownerNationality"] = boatData.ownerNationality
                    fieldsToUpdate["ownerIdNumber"] = boatData.ownerIdNumber
                    boatData.ownerPassportNumber?.let { fieldsToUpdate["ownerPassportNumber"] = it }
                    fieldsToUpdate["ownerMobile"] = boatData.ownerMobile
                    boatData.ownerEmail?.let { fieldsToUpdate["ownerEmail"] = it }
                    boatData.ownerAddress?.let { fieldsToUpdate["ownerAddress"] = it }
                    boatData.ownerCity?.let { fieldsToUpdate["ownerCity"] = it }
                    fieldsToUpdate["ownerCountry"] = boatData.ownerCountry
                    boatData.ownerPostalCode?.let { fieldsToUpdate["ownerPostalCode"] = it }

                    // ✅ NEW: Handle Multiple Owners (if provided by Ministry API)
                    if (!boatData.owners.isNullOrEmpty()) {
                        println("✅ Ministry API returned ${boatData.owners.size} owners - preparing to auto-fill")

                        // ✅ Convert Ministry API format to UI format
                        val uiOwners = boatData.owners.map { apiOwner ->
                            convertApiOwnerToUI(apiOwner)
                        }

                        val ownersJson = Json.encodeToString(uiOwners)
                        fieldsToUpdate["owners"] = ownersJson
                        fieldsToUpdate["totalOwnersCount"] = boatData.totalOwnersCount ?: boatData.owners.size.toString()

                        println("📋 Owners JSON: $ownersJson")
                    } else {
                        println("ℹ️ No multiple owners data from Ministry - using primary owner only")
                    }

                    // ✅ NEW: Handle Engine Information (if provided by Ministry API)
                    if (!boatData.engines.isNullOrEmpty()) {
                        println("✅ Ministry API returned ${boatData.engines.size} engines - preparing to auto-fill")

                        // ✅ Convert Ministry API format to UI format
                        val uiEngines = boatData.engines.map { apiEngine ->
                            convertApiEngineToUI(apiEngine)
                        }

                        val enginesJson = Json.encodeToString(uiEngines)
                        fieldsToUpdate["engines"] = enginesJson

                        println("🔧 Engines JSON: $enginesJson")
                    } else {
                        println("ℹ️ No engine data from Ministry - user will need to add manually")
                    }

                    // Store in accumulated data
                    accumulatedFormData.putAll(fieldsToUpdate)

                    println("✅ Auto-filled ${fieldsToUpdate.size} fields from Ministry data")
                    println("   - Engines: ${boatData.engines?.size ?: 0}")
                    println("   - Owners: ${boatData.owners?.size ?: 0}")

                    FieldFocusResult.UpdateFields(fieldsToUpdate)
                } else {
                    FieldFocusResult.Error("agricultureRequestNumber", "لم يتم العثور على بيانات القارب")
                }
            } else {
                FieldFocusResult.Error(
                    "agricultureRequestNumber",
                    result.exceptionOrNull()?.message ?: "فشل في تحميل بيانات القارب من وزارة الزراعة"
                )
            }
        } catch (e: Exception) {
            println("❌ Error fetching agriculture data: ${e.message}")
            e.printStackTrace()
            FieldFocusResult.Error(
                "agricultureRequestNumber",
                e.message ?: "حدث خطأ غير متوقع"
            )
        }
    }

    /**
     * Convert Ministry API EngineData to UI EngineData format
     */
    private fun convertApiEngineToUI(apiEngine: com.informatique.mtcit.data.repository.EngineData): UIEngineData {
        return UIEngineData(
            id = java.util.UUID.randomUUID().toString(),
            number = apiEngine.engineNumber,
            type = apiEngine.engineType,
            power = apiEngine.enginePower,
            cylinder = apiEngine.cylindersCount,
            manufacturer = apiEngine.manufacturer,
            model = apiEngine.model,
            manufactureYear = apiEngine.manufactureYear,
            productionCountry = apiEngine.producingCountry,
            fuelType = apiEngine.fuelType,
            condition = apiEngine.engineCondition,
            documentUri = "",
            documentName = ""
        )
    }

    /**
     * Convert Ministry API OwnerData to UI OwnerData format
     */
    private fun convertApiOwnerToUI(apiOwner: com.informatique.mtcit.data.repository.OwnerData): UIOwnerData {
        return UIOwnerData(
            id = java.util.UUID.randomUUID().toString(),
            fullName = apiOwner.ownerFullNameAr, // UI uses single fullName field
            nationality = apiOwner.ownerNationality,
            idNumber = apiOwner.ownerIdNumber,
            ownerShipPercentage = apiOwner.ownershipPercentage, // Note: different spelling
            email = apiOwner.ownerEmail,
            mobile = apiOwner.ownerMobile,
            address = apiOwner.ownerAddress,
            city = apiOwner.ownerCity,
            country = apiOwner.ownerCountry,
            postalCode = apiOwner.ownerPostalCode,
            isCompany = apiOwner.companyName.isNotEmpty(), // Set isCompany if company name exists
            companyRegistrationNumber = apiOwner.companyRegistrationNumber,
            companyName = apiOwner.companyName,
            companyType = "", // Ministry API doesn't provide company type
            ownershipProofDocument = "", // Document will be empty initially
            documentName = ""
        )
    }

    /**
     * Validate marine unit selection using TemporaryRegistrationRules
     * Called from MarineRegistrationViewModel when user clicks "Accept & Send" on review step
     */
    suspend fun validateMarineUnitSelection(unitId: String, userId: String): ValidationResult {
        return try {
            println("🔍 TemporaryRegistrationStrategy: Validating unit $unitId using TemporaryRegistrationRules")

            // Find the selected unit
            val selectedUnit = marineUnits.firstOrNull { it.id.toString() == unitId }

            if (selectedUnit == null) {
                println("❌ Unit not found with id: $unitId")
                return ValidationResult.Error("الوحدة البحرية المختارة غير موجودة")
            }

            println("✅ Found unit: ${selectedUnit.name}, id: ${selectedUnit.id}")

            // Use TemporaryRegistrationRules to validate
            val validationResult = temporaryRegistrationRules.validateUnit(selectedUnit, userId)
            val navigationAction = temporaryRegistrationRules.getNavigationAction(validationResult)

            println("✅ Validation result: ${validationResult::class.simpleName}")
            println("✅ Navigation action: ${navigationAction::class.simpleName}")

            ValidationResult.Success(
                validationResult = validationResult,
                navigationAction = navigationAction
            )
        } catch (e: Exception) {
            println("❌ Validation error: ${e.message}")
            e.printStackTrace()
            ValidationResult.Error(e.message ?: "فشل التحقق من حالة الفحص")
        }
    }

    override fun extractCompletedStepsFromApiResponse(response: Any): Set<StepType> {
        // TODO: Parse the Change Name of Ship or Unit API response to determine completed steps
        val completedSteps = mutableSetOf<StepType>()

        println("⚠️ ChangeNameOfShipOrUnitStrategy: extractCompletedStepsFromApiResponse not yet implemented")
        println("   Response type: ${response::class.simpleName}")

        return completedSteps
    }
}
