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
import com.informatique.mtcit.business.transactions.marineunit.rules.MortgageCertificateRules
import com.informatique.mtcit.business.transactions.marineunit.usecases.ValidateMarineUnitUseCase
import com.informatique.mtcit.business.transactions.marineunit.usecases.GetEligibleMarineUnitsUseCase
import com.informatique.mtcit.data.repository.MarineUnitRepository

/**
 * Strategy for Mortgage Certificate Issuance
 * Steps:
 * 1. Person Type Selection (Individual/Company)
 * 2. Commercial Registration (conditional - only for Company)
 * 3. Unit Selection (choose from user's ships) - WITH BUSINESS VALIDATION
 * 4. Mortgage Data (bank info and mortgage details)
 * 5. Review
 */
class MortgageCertificateStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val companyRepository: CompanyRepo,
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository,
    private val mortgageRules: MortgageCertificateRules,
    private val validateMarineUnitUseCase: ValidateMarineUnitUseCase,
    private val getEligibleUnitsUseCase: GetEligibleMarineUnitsUseCase,
    private val marineUnitRepository: MarineUnitRepository
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

        marineUnits = marineUnitRepository.getUserMarineUnits("currentUserId")

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

            // Step 3: Marine Unit Selection - WITH BUSINESS RULES
            SharedSteps.marineUnitSelectionStep(
                units = marineUnits,
                allowMultipleSelection = mortgageRules.allowMultipleSelection(),
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

    suspend fun validateMarineUnitSelection(unitId: String, userId: String): ValidationResult {
        val unit = marineUnits.find { it.id == unitId }
            ?: return ValidationResult.Error("الوحدة البحرية غير موجودة")

        // SIMULATION: استدعاء بيانات السفينة ومراجعة سجل الالتزام
        // Simulate API call to retrieve ship data and review compliance record
        val complianceCheckResult = simulateComplianceRecordCheck(unit, userId)

        // If compliance check finds blocking issues, return ShowComplianceDetailScreen
        if (complianceCheckResult.hasBlockingIssues()) {
            return ValidationResult.Success(
                validationResult = complianceCheckResult.validationResult,
                navigationAction = com.informatique.mtcit.business.transactions.marineunit.MarineUnitNavigationAction.ShowComplianceDetailScreen(
                    marineUnit = unit,
                    complianceIssues = complianceCheckResult.issues,
                    rejectionReason = complianceCheckResult.rejectionReason,
                    rejectionTitle = "تم رفض الطلب - مشاكل في سجل الالتزام"
                )
            )
        }

        // Otherwise, proceed with normal validation
        val (validationResult, navigationAction) = validateMarineUnitUseCase.executeAndGetAction(
            unit = unit,
            userId = userId,
            rules = mortgageRules
        )

        return ValidationResult.Success(validationResult, navigationAction)
    }

    /**
     * SIMULATION: Simulates calling "استدعاء بيانات السفينة ومراجعة سجل الالتزام"
     * This represents the API that retrieves full marine unit data and checks compliance record
     */
    private fun simulateComplianceRecordCheck(
        unit: MarineUnit,
        userId: String
    ): ComplianceCheckResult {
        // SIMULATION SCENARIOS - Using EXACT maritime IDs to avoid false positives:
        // 1. Maritime ID "470123456" (first unit) - simulate violations found
        // 2. Maritime ID "OMN000123" - simulate debts found
        // 3. Maritime ID "OMN000999" - simulate detention found
        // 4. All others - proceed normally (NO ISSUES)

        val issues = mutableListOf<com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue>()

        // Scenario 1: Violations - ONLY for exact maritime ID "470123456"
        if (unit.maritimeId == "470123456") {
            issues.add(
                com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue(
                    category = "المخالفات",
                    title = "وجود مخالفات نشطة",
                    description = "تم رصد 3 مخالفات نشطة على هذه الوحدة البحرية تمنع استكمال المعاملة",
                    severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING,
                    details = mapOf(
                        "عدد المخالفات" to "3",
                        "تاريخ آخر مخالفة" to "2024-10-15",
                        "نوع المخالفة" to "مخالفة سلامة بحرية"
                    )
                )
            )
        }

        // Scenario 2: Debts - for maritime IDs containing "OMN000123"
        if (unit.maritimeId == "OMN000123") {
            issues.add(
                com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue(
                    category = "الديون والمستحقات",
                    title = "وجود ديون مستحقة",
                    description = "يوجد مبلغ مستحق غير مسدد يجب تسديده قبل المتابعة",
                    severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING,
                    details = mapOf(
                        "المبلغ المستحق" to "2,500 ريال عماني",
                        "نوع المستحق" to "رسوم تجديد سنوية",
                        "تاريخ الاستحقاق" to "2024-09-01"
                    )
                )
            )
        }

        // Scenario 3: Detention - for maritime ID "OMN000999"
//        if (unit.maritimeId == "OMN000999") {
//            issues.add(
//                com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue(
//                    category = "الاحتجازات",
//                    title = "الوحدة محتجزة",
//                    description = "الوحدة البحرية محتجزة حالياً ولا يمكن استغلالها",
//                    severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING,
//                    details = mapOf(
//                        "سبب الاحتجاز" to "مخالفة أمنية",
//                        "تاريخ الاحتجاز" to "2024-11-01",
//                        "الجهة المحتجزة" to "خفر السواحل"
//                    )
//                )
//            )
//        }

        // Build rejection reason
        val rejectionReason = if (issues.isNotEmpty()) {
            buildString {
                append("تم رفض طلبكم بسبب: ")
                issues.forEach { issue ->
                    append("\n• ${issue.title}")
                }
                append("\n\nيرجى حل هذه المشاكل أولاً ثم المحاولة مرة أخرى.")
            }
        } else {
            ""
        }

        return ComplianceCheckResult(
            issues = issues,
            rejectionReason = rejectionReason,
            validationResult = if (issues.isEmpty()) {
                com.informatique.mtcit.business.transactions.marineunit.MarineUnitValidationResult.Eligible(
                    unit = unit,
                    additionalData = emptyMap()
                )
            } else {
                com.informatique.mtcit.business.transactions.marineunit.MarineUnitValidationResult.Ineligible.CustomError(
                    unit = unit,
                    reason = rejectionReason,
                    suggestion = "يرجى حل المشاكل المذكورة أعلاه قبل المتابعة"
                )
            }
        )
    }

    /**
     * Result of compliance record check
     */
    private data class ComplianceCheckResult(
        val issues: List<com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue>,
        val rejectionReason: String,
        val validationResult: com.informatique.mtcit.business.transactions.marineunit.MarineUnitValidationResult
    ) {
        fun hasBlockingIssues(): Boolean {
            return issues.any { it.severity == com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING }
        }
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
