package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.R
import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.ui.components.PersonType
import com.informatique.mtcit.ui.components.SelectableItem
import com.informatique.mtcit.ui.repo.CompanyRepo
import com.informatique.mtcit.ui.viewmodels.StepData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Strategy for Mortgage Certificate Issuance
 * Steps:
 * 1. Person Type Selection (Individual/Company)
 * 2. Commercial Registration (conditional - only for Company)
 * 3. Unit Selection (choose from user's ships)
 * 4. Mortgage Data (bank info and mortgage details)
 * 5. Review
 */
class MortgageCertificateStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val companyRepository: CompanyRepo,
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

            // Step 4: Mortgage Data
            StepData(
                titleRes = R.string.mortgage_data,
                descriptionRes = R.string.mortgage_data_desc,
                fields = listOf(
                    FormField.DropDown(
                        id = "bankName",
                        labelRes = R.string.bank_name,
                        options = listOf(
                            "Bank Muscat",
                            "National Bank of Oman",
                            "Bank Dhofar",
                            "Sohar International Bank",
                            "Oman Arab Bank",
                            "HSBC Oman",
                            "Ahli Bank",
                            "Bank Nizwa"
                        ),
                        mandatory = true
                    ),
                    FormField.TextField(
                        id = "mortgageContractNumber",
                        labelRes = R.string.mortgage_contract_number,
                        placeholder = "Enter mortgage contract number",
                        isNumeric = true,
                        mandatory = true
                    ),
                    FormField.DropDown(
                        id = "mortgagePurpose",
                        labelRes = R.string.mortgage_purpose,
                        options = listOf(
                            "Purchase Financing",
                            "Refinancing",
                            "Modification/Upgrade",
                            "Business Loan",
                            "Other"
                        ),
                        mandatory = true
                    ),
                    FormField.TextField(
                        id = "mortgageValue",
                        labelRes = R.string.mortgage_value,
                        placeholder = "Enter mortgage value in OMR",
                        isNumeric = true,
                        mandatory = true
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
                    FormField.FileUpload(
                        id = "mortgageApplication",
                        labelRes = R.string.mortgage_application,
                        allowedTypes = listOf("pdf", "jpg", "jpeg", "png", "doc", "docx"),
                        maxSizeMB = 5,
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
        // Handle person type change to show/hide commercial registration step
        if (fieldId == "selectionPersonType") {
            val mutableFormData = formData.toMutableMap()
            when (value) {
                "INDIVIDUAL" -> {
                    // Clear company-related fields if switching to individual
                    mutableFormData.remove("companyRegistrationNumber")
                    mutableFormData.remove("companyName")
                    mutableFormData.remove("companyType")
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
