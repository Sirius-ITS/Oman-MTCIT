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

    override suspend fun loadDynamicOptions(): Map<String, List<String>> {
        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()

        portOptions = ports
        countryOptions = countries

        return mapOf(
            "registrationPort" to ports,
            "bankCountry" to countries
        )
    }

    override fun getSteps(): List<StepData> {
        return listOf(
            // Step 1: Mortgage Information
            StepData(
                titleRes = R.string.mortgage_information,
                descriptionRes = R.string.mortgage_information_desc,
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
                    ),
                    FormField.TextField(
                        id = "mortgageReferenceNumber",
                        labelRes = R.string.mortgage_reference_number,
                        mandatory = true
                    ),
                    FormField.DatePicker(
                        id = "mortgageDate",
                        labelRes = R.string.mortgage_date,
                        allowPastDates = true,
                        mandatory = true
                    )
                )
            ),
            // Step 2: Bank Release Information
            StepData(
                titleRes = R.string.bank_release_info,
                descriptionRes = R.string.bank_release_info_desc,
                fields = listOf(
                    FormField.TextField(
                        id = "bankName",
                        labelRes = R.string.bank_name,
                        mandatory = true
                    ),
                    FormField.DropDown(
                        id = "bankCountry",
                        labelRes = R.string.bank_country,
                        mandatory = true,
                        options = countryOptions
                    ),
                    FormField.DatePicker(
                        id = "releaseDate",
                        labelRes = R.string.release_date,
                        allowPastDates = true,
                        mandatory = true
                    ),
                    FormField.TextField(
                        id = "releaseReason",
                        labelRes = R.string.release_reason,
                        mandatory = false,
                    )
                )
            ),
            // Step 3: Release Documents
            SharedSteps.documentsStep(
                requiredDocuments = listOf(
                    DocumentConfig(
                        id = "mortgageCertificate",
                        labelRes = R.string.original_mortgage_certificate,
                        mandatory = true
                    ),
                    DocumentConfig(
                        id = "bankReleaseLetter",
                        labelRes = R.string.bank_release_letter,
                        mandatory = true
                    ),
                    DocumentConfig(
                        id = "paymentProof",
                        labelRes = R.string.payment_proof,
                        mandatory = true
                    ),
                    DocumentConfig(
                        id = "ownerIdDocument",
                        labelRes = R.string.identity_document,
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

    override fun processStepData(step: Int, data: Map<String, String>): Map<String, String> {
        return data
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        return repository.submitRegistration(data)
    }
}

