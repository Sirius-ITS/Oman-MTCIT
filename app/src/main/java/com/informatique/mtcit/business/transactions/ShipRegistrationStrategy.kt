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
 * Strategy for Ship Registration transaction - UPDATED to use SharedSteps
 * Uses reusable step templates from SharedSteps object
 * Note: No @Singleton scope - created by factory as needed
 */
class ShipRegistrationStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val companyRepository: CompanyRepo,
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository // NEW: For dynamic dropdowns
) : TransactionStrategy {

    // Cache for loaded dropdown options
    private var portOptions: List<String> = emptyList()
    private var countryOptions: List<String> = emptyList()
    private var shipTypeOptions: List<String> = emptyList()

    override suspend fun loadDynamicOptions(): Map<String, List<String>> {
        // Load all dropdown options from API
        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val shipTypes = lookupRepository.getShipTypes().getOrNull() ?: emptyList()

        // Cache the options for use in getSteps()
        portOptions = ports
        countryOptions = countries
        shipTypeOptions = shipTypes

        return mapOf(
            "registrationPort" to ports,
            "ownerNationality" to countries,
            "ownerCountry" to countries,
            "registrationCountry" to countries,
            "unitType" to shipTypes
        )
    }

    override fun getSteps(): List<StepData> {
        return listOf(
            // ✅ Using SharedSteps.unitSelectionStep with full configuration
            SharedSteps.unitSelectionStep(
                shipTypes = shipTypeOptions,
                ports = portOptions,
                countries = countryOptions,
                includeIMO = true,
                includeMMSI = true,
                includeManufacturer = true,
                includeProofDocument = true,
                includeConstructionDates = true,
                includeRegistrationCountry = true
            ),

            // ✅ Using SharedSteps.ownerInfoStep with full configuration
            SharedSteps.ownerInfoStep(
                nationalities = countryOptions,
                countries = countryOptions,
                includeCompanyFields = true,
                includePassportNumber = true,
                includePostalCode = true,
                includeEmail = true
            ),

            // ✅ Using SharedSteps.documentsStep with specific requirements
            SharedSteps.documentsStep(
                requiredDocuments = listOf(
                    DocumentConfig(
                        id = "shipbuildingCertificate",
                        labelRes = R.string.shipbuilding_certificate_or_sale_contract,
                        mandatory = true
                    ),
                    DocumentConfig(
                        id = "inspectionDocuments",
                        labelRes = R.string.inspection_documents,
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
        // No additional processing needed for ship registration
        return data
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        return repository.submitRegistration(data)
    }

    override fun handleFieldChange(fieldId: String, value: String, formData: Map<String, String>): Map<String, String> {
        // Handle owner type change
        if (fieldId == "owner_type") {
            val mutableFormData = formData.toMutableMap()
            when (value) {
                "فرد" -> {
                    mutableFormData.remove("companyName")
                    mutableFormData.remove("companyRegistrationNumber")
                }
                "شركة" -> {
                    // Company fields will be shown and are required
                }
                "شراكة" -> {
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
            return FieldFocusResult.Error(
                "companyRegistrationNumber",
                "رقم السجل التجاري يجب أن يكون أكثر من 3 أرقام"
            )
        }

        return try {
            val result = companyRepository.fetchCompanyLookup(registrationNumber)
                .flowOn(Dispatchers.IO)
                .catch { throwable ->
                    throw Exception("حدث خطأ أثناء البحث عن الشركة: ${throwable.message}")
                }
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
                is BusinessState.Error -> {
                    FieldFocusResult.Error("companyRegistrationNumber", result.message)
                }
                is BusinessState.Loading -> {
                    FieldFocusResult.NoAction
                }
            }
        } catch (e: Exception) {
            FieldFocusResult.Error("companyRegistrationNumber", e.message ?: "حدث خطأ غير متوقع")
        }
    }
}
