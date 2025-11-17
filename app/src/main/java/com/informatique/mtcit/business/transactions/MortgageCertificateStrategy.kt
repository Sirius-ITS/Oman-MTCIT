package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.R
import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.DocumentConfig
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.ui.repo.CompanyRepo
import com.informatique.mtcit.ui.viewmodels.StepData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Strategy for Mortgage Certificate
 * DEMONSTRATION: Adds an extra "Bank Information" step to show dynamic step addition
 * Shows how to add completely new step types for different business needs
 */
class MortgageCertificateStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val companyRepository: CompanyRepo,
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository
) : TransactionStrategy {

    private var portOptions: List<String> = emptyList()
    private var countryOptions: List<String> = emptyList()
    private var shipTypeOptions: List<String> = emptyList()

    override suspend fun loadDynamicOptions(): Map<String, List<String>> {
        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val shipTypes = lookupRepository.getShipTypes().getOrNull() ?: emptyList()

        portOptions = ports
        countryOptions = countries
        shipTypeOptions = shipTypes

        return mapOf(
            "registrationPort" to ports,
            "ownerNationality" to countries,
            "ownerCountry" to countries,
            "bankCountry" to countries,
            "unitType" to shipTypes
        )
    }

    override fun getSteps(): List<StepData> {
        return listOf(
            // Step 1: Unit Selection (simplified - some fields removed)
            SharedSteps.unitSelectionStep(
                shipTypes = shipTypeOptions,
                ports = portOptions,
                countries = countryOptions,
                includeIMO = true,
                includeMMSI = false, // 🔴 REMOVED
                includeManufacturer = false, // 🔴 REMOVED
                maritimeactivity = shipTypeOptions,
                includeProofDocument = false, // 🔴 REMOVED
                includeConstructionDates = false, // 🔴 REMOVED
                includeRegistrationCountry = false // 🔴 REMOVED
            ),
            // Step 2: Owner Info (keep full)
            SharedSteps.ownerInfoStep(
                nationalities = countryOptions,
                countries = countryOptions,
                includeCompanyFields = true,
            ),
            // 🆕 Step 3: Bank Information (NEW STEP TYPE)
            StepData(
                titleRes = R.string.bank_information,
                descriptionRes = R.string.bank_information_desc,
                fields = listOf(
                    FormField.TextField(
                        id = "bankName",
                        labelRes = R.string.bank_name,
                        mandatory = true
                    ),
                    FormField.TextField(
                        id = "bankBranch",
                        labelRes = R.string.bank_branch,
                        mandatory = true
                    ),
                    FormField.DropDown(
                        id = "bankCountry",
                        labelRes = R.string.bank_country,
                        options = countryOptions,
                        mandatory = true
                    ),
                    FormField.TextField(
                        id = "mortgageAmount",
                        labelRes = R.string.mortgage_amount,
                        isNumeric = true,
                        mandatory = true
                    ),
                    FormField.DropDown(
                        id = "currency",
                        labelRes = R.string.currency,
                        mandatory = true,
                        options = listOf("OMR", "USD", "EUR", "GBP", "AED", "SAR")
                    ),
                    FormField.DatePicker(
                        id = "mortgageStartDate",
                        labelRes = R.string.mortgage_start_date,
                        allowPastDates = true,
                        mandatory = true
                    ),
                    FormField.DatePicker(
                        id = "mortgageEndDate",
                        labelRes = R.string.mortgage_end_date,
                        allowPastDates = false,
                        mandatory = true
                    ),
                    FormField.TextField(
                        id = "mortgageTerms",
                        labelRes = R.string.mortgage_terms,
                        mandatory = false,
                    )
                )
            ),
            // Step 4: Documents
            SharedSteps.documentsStep(
                requiredDocuments = listOf(
                    DocumentConfig(
                        id = "registrationCertificate",
                        labelRes = R.string.original_registration_certificate,
                        mandatory = true
                    ),
                    DocumentConfig(
                        id = "mortgageAgreement",
                        labelRes = R.string.mortgage_agreement,
                        mandatory = true
                    ),
                    DocumentConfig(
                        id = "bankApprovalLetter",
                        labelRes = R.string.bank_approval_letter,
                        mandatory = true
                    ),
                    DocumentConfig(
                        id = "ownerIdDocument",
                        labelRes = R.string.identity_document,
                        mandatory = true
                    )
                )
            ),
            // Step 5: Review
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

    override fun handleFieldChange(fieldId: String, value: String, formData: Map<String, String>): Map<String, String> {
        if (fieldId == "owner_type") {
            val mutableFormData = formData.toMutableMap()
            when (value) {
                "فرد" -> {
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
            return FieldFocusResult.Error("companyRegistrationNumber", "رقم السجل التجاري يجب أن يكون أكثر من 3 أرقام")
        }

        return try {
            val result = companyRepository.fetchCompanyLookup(registrationNumber)
                .flowOn(Dispatchers.IO)
                .catch { throw Exception("حدث خطأ أثناء البحث عن الشركة: ${it.message}") }
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
                is BusinessState.Error -> FieldFocusResult.Error("companyRegistrationNumber", result.message)
                is BusinessState.Loading -> FieldFocusResult.NoAction
            }
        } catch (e: Exception) {
            FieldFocusResult.Error("companyRegistrationNumber", e.message ?: "حدث خطأ غير متوقع")
        }
    }
}

