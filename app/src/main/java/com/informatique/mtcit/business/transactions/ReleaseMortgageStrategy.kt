package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.R
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.DocumentConfig
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.ui.components.PersonType
import com.informatique.mtcit.ui.components.SelectableItem
import com.informatique.mtcit.ui.viewmodels.StepData
import javax.inject.Inject

/**
 * Strategy for Release Mortgage
 * DEMONSTRATION: Simplified 3-step process showing minimal required data
 * Different document requirements from other transactions
 */
class ReleaseMortgageStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository
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
        marineUnits = listOf(
            MarineUnit(
                id = "1",
                name = "الريادة البحرية",
                type = "سفينة صيد",
                imoNumber = "9990001",
                callSign = "A9BC2",
                maritimeId = "470123456",
                registrationPort = "صحار",
                activity = "صيد",
                isOwned = false
            ),

            MarineUnit(
                id = "3",
                name = "النجم الساطع",
                type = "سفينة شحن",
                imoNumber = "9990002",
                callSign = "B8CD3",
                maritimeId = "470123457",
                registrationPort = "مسقط",
                activity = "شحن دولي",
                isOwned = true // ⚠️ مملوكة - هتظهر مع التحذير
            ),
            MarineUnit(
                id = "8",
                name = "البحر الهادئ",
                type = "سفينة صهريج",
                imoNumber = "9990008",
                callSign = "H8IJ9",
                maritimeId = "470123463",
                registrationPort = "صلالة",
                activity = "نقل وقود",
                isOwned = true // ⚠️ مملوكة
            ),
            MarineUnit(
                id = "9",
                name = "اللؤلؤة البيضاء",
                type = "سفينة سياحية",
                imoNumber = "9990009",
                callSign = "I9JK0",
                maritimeId = "470123464",
                registrationPort = "مسقط",
                activity = "رحلات سياحية",
                isOwned = false
            ),
            MarineUnit(
                id = "10",
                name = "الشراع الذهبي",
                type = "سفينة شراعية",
                imoNumber = "9990010",
                callSign = "J0KL1",
                maritimeId = "470123465",
                registrationPort = "صحار",
                activity = "تدريب بحري",
                isOwned = false
            )
        )

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

            // Step 3: marine Selection
            SharedSteps.marineUnitSelectionStep(
                units = marineUnits,
                allowMultipleSelection = false,
                showOwnedUnitsWarning = true
            ),
            // Step 4: Review
            SharedSteps.reviewStep()
        )
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
