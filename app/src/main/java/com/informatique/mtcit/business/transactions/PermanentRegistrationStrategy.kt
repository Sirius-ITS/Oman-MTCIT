package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.R
import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.transactions.shared.DocumentConfig
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.business.usecases.FormValidationUseCase
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
import javax.inject.Inject

/**
 * Strategy for Permanent Registration Certificate
 * DEMONSTRATION: Adds an extra "Previous Registration" step to show dynamic step addition
 */
class PermanentRegistrationStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val companyRepository: CompanyRepo,
    private val validationUseCase: FormValidationUseCase,
    private val marineUnitRepository: MarineUnitRepository,
    private val lookupRepository: LookupRepository
) : TransactionStrategy {

    // Cache for loaded dropdown options
    private var portOptions: List<String> = emptyList()
    private var countryOptions: List<String> = emptyList()
    private var shipTypeOptions: List<String> = emptyList()
    private var typeOptions: List<PersonType> = emptyList()
    private var commercialOptions: List<SelectableItem> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()


    // ✅ الحل: اعمل cache للـ form data
    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf()


    override suspend fun loadDynamicOptions(): Map<String, List<*>>  {
        // Load all dropdown options from API
        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val shipTypes = lookupRepository.getShipTypes().getOrNull() ?: emptyList()
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()
        val commercialRegistrations = lookupRepository.getCommercialRegistrations().getOrNull() ?: emptyList()

        // ✅ Don't load ships here - they will be loaded when user presses Next
        println("🚢 Skipping initial ship load - will load after user selects type and presses Next")

        portOptions = ports
        countryOptions = countries
        shipTypeOptions = shipTypes
        typeOptions = personTypes
        commercialOptions = commercialRegistrations

        return mapOf(
            "marineUnits" to emptyList<MarineUnit>(), // ✅ Empty initially
            "registrationPort" to ports,
            "ownerNationality" to countries,
            "ownerCountry" to countries,
            "registrationCountry" to countries,
            "unitType" to shipTypes
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

        // ✅ FOR TESTING: Use ownerCivilId for BOTH person types
        // Because current API only returns data when using ownerCivilId filter
        // In production, company should use commercialRegNumber
        val (ownerCivilId, commercialRegNumber) = when (personType) {
            "فرد" -> {
                println("✅ Individual: Using ownerCivilId")
                Pair("12345678", null)
            }
            "شركة" -> {
                println("✅ Company: Using ownerCivilId (FOR TESTING - API doesn't support commercialRegNumber yet)")
                Pair("12345678", null) // ✅ Use ownerCivilId instead of commercialRegNumber for testing
            }
            else -> Pair(null, null)
        }

        println("🔍 Calling loadShipsForOwner with ownerCivilId=$ownerCivilId, commercialRegNumber=$commercialRegNumber")
        println("📋 Note: Using ownerCivilId='12345678' for both person types (API limitation)")

        marineUnits = marineUnitRepository.loadShipsForOwner(ownerCivilId, commercialRegNumber)
        println("✅ Loaded ${marineUnits.size} ships")

        return marineUnits
    }

    override suspend fun clearLoadedShips() {
        println("🧹 Clearing loaded ships cache")
        marineUnits = emptyList()
    }

    override fun updateAccumulatedData(data: Map<String, String>) {
        accumulatedFormData.putAll(data)
        println("📦 PermanentRegistration - Updated accumulated data: $accumulatedFormData")
    }

    override fun getSteps(): List<StepData> {
        val steps = mutableListOf<StepData>()

        steps.add(SharedSteps.personTypeStep(typeOptions))

        // Step 1: Commercial Registration (بس لو اختار شركة)
        val selectedPersonType = accumulatedFormData["selectionPersonType"]

        if (selectedPersonType == "شركة") {  // ⚠️ بيقارن بالـ string "شركة"
            steps.add(SharedSteps.commercialRegistrationStep(commercialOptions))
        }

        steps.add(
            SharedSteps.marineUnitRegistrationCertificateStep(
                showInfoMessage = true
            )
        )
        val hasTemporaryCertificate = accumulatedFormData["hasTemporaryRegistrationCertificate"]

        if (hasTemporaryCertificate == "yes") {
            // ✅ لو "نعم": اتخطى كل الـ steps التقنية وروح على Insurance مباشرة
            println("✅ User has temporary certificate - Skipping technical steps")
            // بس هنضيف Owner Info (مهم للتسجيل)


            steps.add(
                SharedSteps.marineUnitSelectionStep(
                    units = marineUnits,
                    allowMultipleSelection = false, // اختيار وحدة واحدة فقط
                    showAddNewButton = false,
                    showOwnedUnitsWarning = true
                )
            )
            steps.add(
                SharedSteps.insuranceDocumentStep(
                    countries = countryOptions
                )
            )
            steps.add(
                SharedSteps.marineUnitNameSelectionStep(
                    showReservationInfo = true
                )
            )

        } else if (hasTemporaryCertificate == "no") {

//            // ✅ نشيك لو المستخدم ضاف سفينة جديدة او اختار سفينة موجودة
//            val isAddingNewUnit = accumulatedFormData["isAddingNewUnit"] == "true"
//            val selectedUnitsJson = accumulatedFormData["selectedMarineUnits"] ?: "[]"
//            val selectedUnits = try {
//                kotlinx.serialization.json.Json.decodeFromString<List<String>>(selectedUnitsJson)
//            } catch (_: Exception) {
//                emptyList()
//            }
//
//
//            if (isAddingNewUnit || selectedUnits.isEmpty()) {
            steps.add(
                SharedSteps.unitSelectionStep(
                    shipTypes = shipTypeOptions,
                    ports = portOptions,
                    countries = countryOptions,
                    includeIMO = true,
                    includeMMSI = true,
                    includeManufacturer = true,
                    maritimeactivity = shipTypeOptions,
                    includeProofDocument = false,
                    includeConstructionDates = true,
                    includeRegistrationCountry = true
                )
            )

            steps.add(
                SharedSteps.marineUnitDimensionsStep(
                    includeHeight = true,
                    includeDecksCount = true
                )
            )

            steps.add(
                SharedSteps.marineUnitWeightsStep(
                    includeMaxPermittedLoad = true
                )
            )

            steps.add(
                SharedSteps.engineInfoStep(
                    manufacturers = listOf(
                        "Manufacturer 1",
                        "Manufacturer 2",
                        "Manufacturer 3"
                    ),
                    countries = countryOptions,
                    fuelTypes = listOf("Gas 80", "Gas 90", "Gas 95", "Diesel", "Electric"),
                    engineConditions = listOf(
                        "New",
                        "Used - Like New",
                        "Used - Good",
                        "Used - Fair",
                        "Used - Poor"
                    ),
                )
            )
//            }


            steps.add(
                SharedSteps.ownerInfoStep(
                    nationalities = countryOptions,
                    countries = countryOptions,
                    includeCompanyFields = true,
                )
            )
            steps.add(
                SharedSteps.documentsStep(
                    requiredDocuments = listOf(
                        DocumentConfig(
                            id = "shipbuildingCertificate",
                            labelRes = R.string.shipbuilding_certificate_or_sale_contract,
                            mandatory = true
                        ),
                        DocumentConfig(
                            id = "inspectionDocuments",
                            labelRes = R.string.inspection_documents,
                            mandatory = true
                        )
                    )
                )
            )
            steps.add(SharedSteps.reviewStep())
            steps.add(
                SharedSteps.insuranceDocumentStep(
                    countries = countryOptions
                )
            )
        }
            steps.add(
                SharedSteps.marineUnitNameSelectionStep(
                    showReservationInfo = true
                )
            )

        return steps
    }

    override fun validateStep(
        step: Int,
        data: Map<String, Any>
    ): Pair<Boolean, Map<String, String>> {
        val stepData = getSteps().getOrNull(step) ?: return Pair(false, emptyMap())
        val formData = data.mapValues { it.value.toString() }
        return validationUseCase.validateStep(stepData, formData)
    }

    override fun processStepData(step: Int, data: Map<String, String>): Int {
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("💾 Processing Step $step Data: $data")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // ✅ احفظ الـ data في الـ cache
        accumulatedFormData.putAll(data)

        println("📦 Accumulated Data After Update: $accumulatedFormData")

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

