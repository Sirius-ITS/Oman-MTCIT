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
 * Strategy for Captain Name Change transaction - UPDATED to use SharedSteps
 * طلب تغيير اسم الربان أو الوحدة البحرية
 *
 * Demonstrates:
 * - Custom ship identification step
 * - Custom captain information steps (current and new)
 * - Reusing shared documents and review steps
 */
class CaptainNameChangeStrategy @Inject constructor(
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository
) : TransactionStrategy {

    // Cache for loaded dropdown options
    private var countryOptions: List<String> = emptyList()

    override suspend fun loadDynamicOptions(): Map<String, List<String>> {
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        countryOptions = countries

        return mapOf(
            "captainNationality" to countries
        )
    }

    override fun getSteps(): List<StepData> {
        return listOf(
            createShipIdentificationStep(),
            createCurrentCaptainStep(),
            createNewCaptainStep(),

            // ✅ Using SharedSteps.documentsStep with captain-specific requirements
            SharedSteps.documentsStep(
                requiredDocuments = listOf(
                    DocumentConfig(
                        id = "captainLicense",
                        labelRes = R.string.proof_document, // TODO: Add proper string resource for captain license
                        allowedTypes = listOf("pdf", "jpg", "jpeg", "png"),
                        mandatory = true
                    ),
                    DocumentConfig(
                        id = "captainCertificate",
                        labelRes = R.string.inspection_documents, // TODO: Add proper string resource
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

    // ✅ Custom step - Ship identification (specific to captain change)
    private fun createShipIdentificationStep(): StepData = StepData(
        titleRes = R.string.unit_data,
        descriptionRes = R.string.unit_data_description,
        fields = listOf(
            FormField.TextField(
                id = "shipName",
                labelRes = R.string.ship_manufacturer, // TODO: Use proper string resource
                mandatory = true
            ),
            FormField.TextField(
                id = "registrationNumber",
                labelRes = R.string.company_registration_number, // TODO: Use proper string resource
                mandatory = true
            )
        )
    )

    // ✅ Custom step - Current captain info
    private fun createCurrentCaptainStep(): StepData = StepData(
        titleRes = R.string.owner_info,
        descriptionRes = R.string.owner_info_description,
        fields = listOf(
            FormField.TextField(
                id = "currentCaptainName",
                labelRes = R.string.owner_full_name,
                mandatory = true
            ),
            FormField.TextField(
                id = "currentCaptainId",
                labelRes = R.string.owner_id_number,
                mandatory = true
            )
        )
    )

    // ✅ Custom step - New captain info
    private fun createNewCaptainStep(): StepData = StepData(
        titleRes = R.string.owner_info,
        descriptionRes = R.string.owner_info_description,
        fields = listOf(
            FormField.TextField(
                id = "newCaptainName",
                labelRes = R.string.owner_full_name,
                mandatory = true
            ),
            FormField.TextField(
                id = "newCaptainId",
                labelRes = R.string.owner_id_number,
                mandatory = true
            ),
            FormField.TextField(
                id = "newCaptainEmail",
                labelRes = R.string.email,
                mandatory = true
            ),
            FormField.TextField(
                id = "newCaptainMobile",
                labelRes = R.string.owner_mobile,
                mandatory = true
            )
        )
    )
}
