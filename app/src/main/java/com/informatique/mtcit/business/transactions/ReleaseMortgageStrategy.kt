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

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()
        val commercialRegistrations = lookupRepository.getCommercialRegistrations().getOrNull() ?: emptyList()

        portOptions = ports
        countryOptions = countries
        personTypeOptions = personTypes
        commercialOptions = commercialRegistrations

        marineUnits = marineUnitRepository.getUserMarineUnits("currentUserId")

        return mapOf(
            "registrationPort" to ports,
            "ownerNationality" to countries,
            "ownerCountry" to countries,
            "bankCountry" to countries,
            "marineUnits" to marineUnits.map { it.maritimeId },
            "commercialRegistration" to commercialRegistrations,
            "personType" to personTypes
        )
    }

    override fun getSteps(): List<StepData> {
        return listOf(
            // Step 1: Person Type Selection
            SharedSteps.personTypeStep(
                options = personTypeOptions
            ),

            // Step 2: Commercial Registration (conditional)
            SharedSteps.commercialRegistrationStep(
                options = commercialOptions
            ),

            // Step 3: Marine Unit Selection - WITH BUSINESS RULES
            // Only mortgaged units should be selectable
            SharedSteps.marineUnitSelectionStep(
                units = marineUnits,
                allowMultipleSelection = releaseMortgageRules.allowMultipleSelection(),
                showOwnedUnitsWarning = true
            ),

            // Step 4: Review (mortgage details will be shown from backend)
            SharedSteps.reviewStep()
        )
    }

    // NEW: Validate marine unit selection with business rules
    suspend fun validateMarineUnitSelection(unitId: String, userId: String): ValidationResult {
        val unit = marineUnits.find { it.id == unitId }
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

    override fun processStepData(step: Int, data: Map<String, String>): Int {
        return step
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        return repository.submitRegistration(data)
    }
}
