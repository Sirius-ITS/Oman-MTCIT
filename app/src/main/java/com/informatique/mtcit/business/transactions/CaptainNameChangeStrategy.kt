package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.R
import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.DocumentConfig
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.common.FormField
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
 * Strategy for Captain Name Change transaction - UPDATED to use SharedSteps
 * طلب تغيير اسم الربان أو الوحدة البحرية
 *
 * Demonstrates:
 * - Custom ship identification step
 * - Custom captain information steps (current and new)
 * - Reusing shared documents and review steps
 */
class CaptainNameChangeStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val companyRepository: CompanyRepo,
    private val validationUseCase: FormValidationUseCase,
    private val marineUnitRepository: MarineUnitRepository,
    private val lookupRepository: LookupRepository
) : TransactionStrategy {
    private var portOptions: List<String> = emptyList()
    private var countryOptions: List<String> = emptyList()
    private var shipTypeOptions: List<String> = emptyList()
    private var commercialOptions: List<SelectableItem> = emptyList()
    private var typeOptions: List<PersonType> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()
    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf() // ✅ Track form data

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        // Load all dropdown options from API
        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val shipTypes = lookupRepository.getShipTypes().getOrNull() ?: emptyList()
        val commercialRegistrations = lookupRepository.getCommercialRegistrations().getOrNull() ?: emptyList()
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()

        // Cache the options for use in getSteps()
        portOptions = ports
        countryOptions = countries
        shipTypeOptions = shipTypes
        commercialOptions = commercialRegistrations
        typeOptions = personTypes

        println("🚢 Skipping initial ship load - will load after user selects type and presses Next")

        return mapOf(
            "marineUnits" to emptyList<MarineUnit>(),
            "registrationPort" to ports,
            "ownerNationality" to countries,
            "ownerCountry" to countries,
            "registrationCountry" to countries,
            "unitType" to shipTypes
        )
    }

    override suspend fun loadShipsForSelectedType(formData: Map<String, String>): List<MarineUnit> {
        val personType = formData["selectionPersonType"]
        // ✅ FIXED: The actual field ID is "selectionData" not "commercialRegistration"
        val commercialReg = formData["selectionData"]

        println("🚢 loadShipsForSelectedType called - personType=$personType, commercialReg=$commercialReg")

        // ✅ FOR TESTING: Use ownerCivilId for BOTH person types
        val (ownerCivilId, commercialRegNumber) = when (personType) {
            "فرد" -> {
                println("✅ Individual: Using ownerCivilId")
                Pair("12345678", null)
            }
            "شركة" -> {
                println("✅ Company: Using ownerCivilId (FOR TESTING - API doesn't support commercialRegNumber yet)")
                Pair("12345678", null)
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
        println("📦 CancelRegistration - Updated accumulated data: $accumulatedFormData")
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


        // Step 5: Review
        steps.add(SharedSteps.reviewStep())

        return steps
    }

    override fun validateStep(step: Int, data: Map<String, Any>): Pair<Boolean, Map<String, String>> {
        val stepData = getSteps().getOrNull(step) ?: return Pair(false, emptyMap())
        val formData = data.mapValues { it.value.toString() }
        return validationUseCase.validateStep(stepData, formData)
    }

    override suspend fun processStepData(step: Int, data: Map<String, String>): Int {
        println("🔄 processStepData called with: $data")

        // ✅ Accumulate form data for dynamic step logic
        accumulatedFormData.putAll(data)
        println("📦 CancelRegistration - Accumulated data: $accumulatedFormData")
        return step
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
}
