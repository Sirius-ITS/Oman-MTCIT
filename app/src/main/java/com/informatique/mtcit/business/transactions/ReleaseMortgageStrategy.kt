package com.informatique.mtcit.business.transactions

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

/**
 * Strategy for Release Mortgage
 * Steps:
 * 1. Person Type Selection (Individual/Company)
 * 2. Commercial Registration (conditional - only for Company)
 * 3. Unit Selection (choose from mortgaged ships) - WITH BUSINESS VALIDATION
 * 4. Review
 */
class ReleaseMortgageStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository,
    private val releaseMortgageRules: ReleaseMortgageRules,
    private val validateMarineUnitUseCase: ValidateMarineUnitUseCase,
    private val getEligibleUnitsUseCase: GetEligibleMarineUnitsUseCase,
    private val marineUnitRepository: MarineUnitRepository
) : TransactionStrategy {

    private var portOptions: List<String> = emptyList()
    private var countryOptions: List<String> = emptyList()
    private var personTypeOptions: List<PersonType> = emptyList()
    private var commercialOptions: List<SelectableItem> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()
    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf()

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()
        val commercialRegistrations = lookupRepository.getCommercialRegistrations().getOrNull() ?: emptyList()

        portOptions = ports
        countryOptions = countries
        personTypeOptions = personTypes
        commercialOptions = commercialRegistrations

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

        // Step 4: Review
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
        return step
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        return repository.submitRegistration(data)
    }
}
