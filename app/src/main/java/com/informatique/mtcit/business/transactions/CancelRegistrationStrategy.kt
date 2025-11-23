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
 * Strategy for Cancel Permanent Registration (Deletion/Removal)
 * DEMONSTRATION: Highly simplified - minimal fields, just cancellation info
 * Shows extreme case of field removal for streamlined process
 */
class CancelRegistrationStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository
) : TransactionStrategy {

    private var portOptions: List<String> = emptyList()

    override suspend fun loadDynamicOptions(): Map<String, List<String>> {
        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
        portOptions = ports

        return mapOf("registrationPort" to ports)
    }

    override fun getSteps(): List<StepData> {
        return listOf(
            // Step 1: Registration to Cancel
            StepData(
                titleRes = R.string.registration_to_cancel,
                descriptionRes = R.string.registration_to_cancel_desc,
                fields = listOf(
                    FormField.TextField(
                        id = "registrationNumber",
                        labelRes = R.string.registration_number,
                        mandatory = true
                    ),
                    FormField.TextField(
                        id = "vesselName",
                        labelRes = R.string.vessel_name,
                        mandatory = true
                    ),
                    FormField.DropDown(
                        id = "registrationPort",
                        labelRes = R.string.registration_port,
                        mandatory = true,
                        options = portOptions
                    )
                )
            ),
            // Step 2: Cancellation Reason
            StepData(
                titleRes = R.string.cancellation_reason,
                descriptionRes = R.string.cancellation_reason_desc,
                fields = listOf(
                    FormField.DropDown(
                        id = "cancellationReason",
                        labelRes = R.string.reason_for_cancellation,
                        mandatory = true,
                        options = listOf(
                            "بيع السفينة",
                            "تفكيك السفينة",
                            "فقدان السفينة",
                            "نقل التسجيل لدولة أخرى",
                            "غرق السفينة",
                            "أخرى"
                        )
                    ),
                    FormField.TextField(
                        id = "cancellationDetails",
                        labelRes = R.string.cancellation_details,
                        mandatory = false,
                    ),
                    FormField.DatePicker(
                        id = "effectiveDate",
                        labelRes = R.string.effective_date,
                        allowPastDates = false,
                        mandatory = true
                    )
                )
            ),
            // Step 3: Documents (minimal)
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
                        id = "cancellationProof",
                        labelRes = R.string.cancellation_proof,
                        mandatory = true
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

    override fun processStepData(step: Int, data: Map<String, String>): Int {
        return step
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        return repository.submitRegistration(data)
    }
}

