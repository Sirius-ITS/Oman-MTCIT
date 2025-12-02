package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.R
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.DocumentConfig
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.ui.viewmodels.StepData
import javax.inject.Inject

/**
 * Strategy for Ship Dimensions Change transaction - UPDATED to use SharedSteps
 * طلب تغيير أبعاد السفينة أو الوحدة البحرية
 *
 * Demonstrates:
 * - Custom ship identification step
 * - Custom dimension steps (current and new)
 * - Reusing shared documents and review steps
 */
class ShipDimensionsChangeStrategy @Inject constructor(
    private val validationUseCase: FormValidationUseCase
    // Note: No LookupRepository needed - no dynamic dropdowns for dimension change
) : TransactionStrategy {

    override suspend fun loadDynamicOptions(): Map<String, List<String>> {
        // No dynamic dropdowns needed for dimension change
        return emptyMap()
    }

    override fun getSteps(): List<StepData> {
        return listOf(
            createShipIdentificationStep(),
            createCurrentDimensionsStep(),
            createNewDimensionsStep(),

            // ✅ Using SharedSteps.documentsStep with dimension-specific requirements
            SharedSteps.documentsStep(
                requiredDocuments = listOf(
                    DocumentConfig(
                        id = "engineeringReport",
                        labelRes = R.string.proof_document, // TODO: Add proper string resource
                        allowedTypes = listOf("pdf", "jpg", "jpeg", "png", "doc", "docx", "xls", "xlsx", "txt"),
                        mandatory = true
                    ),
                    DocumentConfig(
                        id = "inspectionCertificate",
                        labelRes = R.string.inspection_documents,
                        allowedTypes = listOf("pdf", "jpg", "jpeg", "png", "doc", "docx", "xls", "xlsx", "txt"),
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

    override suspend fun processStepData(step: Int, data: Map<String, String>): Int {
        return step
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        // TODO: Implement actual submission when repository is ready
        return Result.success(true)
    }

    // ✅ Custom step - Ship identification (specific to dimension change)
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

    // ✅ Custom step - Current dimensions
    private fun createCurrentDimensionsStep(): StepData = StepData(
        titleRes = R.string.marine_unit_Dimentions,
        descriptionRes = R.string.marine_unit_Dimentions,
        fields = listOf(
            FormField.TextField(
                id = "currentLength",
                labelRes = R.string.owner_full_name_ar +R.string.owner_full_name_en, // TODO: Use proper string resource for "Length"
                isNumeric = true,
                mandatory = true
            ),
            FormField.TextField(
                id = "currentWidth",
                labelRes = R.string.owner_full_name_ar +R.string.owner_full_name_en , // TODO: Use proper string resource for "Width"
                isNumeric = true,
                mandatory = true
            ),
            FormField.TextField(
                id = "currentHeight",
                labelRes = R.string.owner_full_name_ar +R.string.owner_full_name_en, // TODO: Use proper string resource for "Height"
                isNumeric = true,
                mandatory = true
            )
        )
    )

    // ✅ Custom step - New dimensions
    private fun createNewDimensionsStep(): StepData = StepData(
        titleRes = R.string.marine_unit_Dimentions,
        descriptionRes = R.string.marine_unit_Dimentions,
        fields = listOf(
            FormField.TextField(
                id = "newLength",
                labelRes = R.string.owner_full_name_ar +R.string.owner_full_name_en, // TODO: Use proper string resource for "New Length"
                isNumeric = true,
                mandatory = true
            ),
            FormField.TextField(
                id = "newWidth",
                labelRes =R.string.owner_full_name_ar +R.string.owner_full_name_en, // TODO: Use proper string resource for "New Width"
                isNumeric = true,
                mandatory = true
            ),
            FormField.TextField(
                id = "newHeight",
                labelRes = R.string.owner_full_name_ar +R.string.owner_full_name_en, // TODO: Use proper string resource for "New Height"
                isNumeric = true,
                mandatory = true
            )
        )
    )
}
