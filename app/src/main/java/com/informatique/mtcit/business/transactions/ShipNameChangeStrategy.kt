package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.R
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.DocumentConfig
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.ui.viewmodels.StepData
import javax.inject.Inject

/**
 * Strategy for Ship Name Change transaction - UPDATED to use SharedSteps
 * طلب تغيير اسم السفينة أو الوحدة البحرية
 *
 * Demonstrates:
 * - Reusing ownerInfoStep with different configuration (no passport, no postal code)
 * - Adding transaction-specific fields (reason for name change)
 * - Using shared documents step with different requirements
 */
class ShipNameChangeStrategy @Inject constructor(
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository
) : TransactionStrategy {

    // Cache for loaded dropdown options
    private var countryOptions: List<String> = emptyList()

    override suspend fun loadDynamicOptions(): Map<String, List<String>> {
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        countryOptions = countries

        return mapOf(
            "ownerNationality" to countries,
            "ownerCountry" to countries
        )
    }

    override fun getSteps(): List<StepData> {
        return listOf(
            createShipIdentificationStep(),

            // ✅ Reusing SharedSteps.ownerInfoStep with DIFFERENT configuration
            SharedSteps.ownerInfoStep(
                nationalities = countryOptions,
                countries = countryOptions,
                includeCompanyFields = true
            ),

            // ✅ Using SharedSteps.documentsStep with name-change-specific requirements
            SharedSteps.documentsStep(
                requiredDocuments = listOf(
                    DocumentConfig(
                        id = "nameChangeJustification",
                        labelRes = R.string.proof_document, // TODO: Add proper string resource
                        mandatory = true
                    ),
                    DocumentConfig(
                        id = "ownershipProof",
                        labelRes = R.string.ownership_certificate,
                        mandatory = true
                    )
                )
            ),

            // ✅ Using SharedSteps.reviewStep
            SharedSteps.reviewStep()
        )
    }

    override fun validateStep(step: Int, data: Map<String, Any>): Pair<Boolean, Map<String, String>> {
        val stepData = getSteps().getOrNull(step) ?: return Pair(false, emptyMap())
        val formData = data.mapValues { it.value.toString() }
        return validationUseCase.validateStep(stepData, formData)
    }

    override fun processStepData(step: Int, data: Map<String, String>): Map<String, String> {
        return data
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        // TODO: Implement actual submission when repository is ready
        return Result.success(true)
    }

    // ✅ Custom step for ship identification (not shared - specific to name change)
    private fun createShipIdentificationStep(): StepData = StepData(
        titleRes = R.string.unit_data,
        descriptionRes = R.string.unit_data_description,
        fields = listOf(
            FormField.TextField(
                id = "currentShipName",
                labelRes = R.string.ship_manufacturer, // TODO: Use proper string resource
                mandatory = true
            ),
            FormField.TextField(
                id = "registrationNumber",
                labelRes = R.string.company_registration_number, // TODO: Use proper string resource
                mandatory = true
            ),
            FormField.TextField(
                id = "newShipName",
                labelRes = R.string.ship_manufacturer, // TODO: Use proper string resource for "New Ship Name"
                mandatory = true
            )
        )
    )
}
