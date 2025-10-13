package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.R
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.DocumentConfig
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.ui.viewmodels.StepData
import javax.inject.Inject

/**
 * Strategy for Suspend Permanent Registration
 * DEMONSTRATION: Simplified form - removes many optional fields to show dynamic field removal
 * Only requires: existing registration info, reason, and supporting documents
 */
class SuspendRegistrationStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository
) : TransactionStrategy {

    private var portOptions: List<String> = emptyList()
    private var countryOptions: List<String> = emptyList()

    override suspend fun loadDynamicOptions(): Map<String, List<String>> {
        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()

        portOptions = ports
        countryOptions = countries

        return mapOf(
            "registrationPort" to ports,
            "registrationCountry" to countries
        )
    }

    override fun getSteps(): List<StepData> {
        return listOf(
            // Step 1: Existing Registration Information (simplified)
            StepData(
                titleRes = R.string.existing_registration_info,
                descriptionRes = R.string.existing_registration_info_desc,
                fields = listOf(
                    FormField.TextField(
                        id = "vesselName",
                        labelRes = R.string.vessel_name,
                        mandatory = true
                    ),
                    FormField.TextField(
                        id = "registrationNumber",
                        labelRes = R.string.registration_number,
                        mandatory = true
                    ),
                    FormField.DropDown(
                        id = "registrationPort",
                        labelRes = R.string.registration_port,
                        mandatory = true,
                        options = portOptions
                    ),
                    FormField.TextField(
                        id = "imoNumber",
                        labelRes = R.string.imo_number,
                        mandatory = false
                    )
                )
            ),
            // Step 2: Suspension Reason
            StepData(
                titleRes = R.string.suspension_reason,
                descriptionRes = R.string.suspension_reason_desc,
                fields = listOf(
                    FormField.DropDown(
                        id = "suspensionReason",
                        labelRes = R.string.reason_for_suspension,
                        mandatory = true,
                        options = listOf(
                            "تحت الصيانة",
                            "عدم الاستخدام المؤقت",
                            "نزاع قانوني",
                            "أسباب مالية",
                            "أخرى"
                        )
                    ),
                    FormField.TextField(
                        id = "suspensionDetails",
                        labelRes = R.string.additional_details,
                        mandatory = true
                    ),
                    FormField.DatePicker(
                        id = "requestedSuspensionDate",
                        labelRes = R.string.requested_suspension_date,
                        allowPastDates = true,
                        mandatory = true
                    )
                )
            ),
            // Step 3: Supporting Documents (simplified)
            SharedSteps.documentsStep(
                requiredDocuments = listOf(
                    DocumentConfig(
                        id = "registrationCertificate",
                        labelRes = R.string.original_registration_certificate,
                        mandatory = true
                    ),
                    DocumentConfig(
                        id = "ownerIdDocument",
                        labelRes = R.string.identity_document,
                        mandatory = true
                    ),
                    DocumentConfig(
                        id = "supportingDocuments",
                        labelRes = R.string.supporting_documents,
                        mandatory = false
                    )
                )
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

    override fun processStepData(step: Int, data: Map<String, String>): Map<String, String> {
        return data
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        return repository.submitRegistration(data)
    }
}

