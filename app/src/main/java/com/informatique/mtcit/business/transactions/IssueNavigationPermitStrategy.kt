package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.navigation.NavRoutes
import com.informatique.mtcit.navigation.NavigationManager
import com.informatique.mtcit.ui.components.PersonType
import com.informatique.mtcit.ui.components.SelectableItem
import com.informatique.mtcit.ui.repo.CompanyRepo
import com.informatique.mtcit.ui.screens.RequestDetail.CheckShipCondition
import com.informatique.mtcit.ui.viewmodels.StepData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Strategy for Temporary Registration Certificate
 * Full baseline implementation with all steps
 */
class IssueNavigationPermitStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val companyRepository: CompanyRepo,
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository,
    private val navigationManager: NavigationManager
) : TransactionStrategy {

    private var countryOptions: List<String> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()

    private var commercialOptions: List<SelectableItem> = emptyList()

    private var typeOptions: List<PersonType> = emptyList()

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val commercialRegistrations = lookupRepository.getCommercialRegistrations().getOrNull() ?: emptyList()
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()

        countryOptions = countries
        commercialOptions = commercialRegistrations
        typeOptions = personTypes


        return mapOf(
            "marineUnits" to marineUnits.map { it.maritimeId },
            "registrationCountry" to countries,
            "commercialRegistration" to commercialRegistrations,
            "personType" to personTypes
        )
    }

    override fun getSteps(): List<StepData> {
        return listOf(
            // User type
            SharedSteps.personTypeStep(options = typeOptions),

            SharedSteps.commercialRegistrationStep(commercialOptions),

            SharedSteps.marineUnitSelectionStep(
                units = marineUnits,
                allowMultipleSelection = false, // اختيار وحدة واحدة فقط
                showOwnedUnitsWarning = true
            ),

            SharedSteps.sailorInfoStep(
                jobs = listOf("Captain", "Chief Engineer", "Boatswain",
                    "Electro-Technical Officer", "Navigator", "Chief Medical Officer")
            ),

            SharedSteps.reviewStep()
        )
    }

    override fun validateStep(step: Int, data: Map<String, Any>): Pair<Boolean, Map<String, String>> {
        val stepData = getSteps().getOrNull(step) ?: return Pair(false, emptyMap())
        val formData = data.mapValues { it.value.toString() }
        return validationUseCase.validateStep(stepData, formData)
    }

    override fun processStepData(step: Int, data: Map<String, String>): Int {
        if (step == 0 && data.filterValues { it == "فرد" }.isNotEmpty()){
            return 2
        } else if (step == 2 && data.filterValues { it == "[\"470123456\"]" }.isNotEmpty()){
            /*val shipData = mapOf(
                "نوع الوحدة البحرية" to "سفينة صيد",
                "رقم IMO" to "9990001",
                "رمز النداء" to "A9BC2",
                "رقم الهوية البحرية" to "470123456",
                "ميناء التسجيل" to "صحار",
                "النشاط البحري" to "صيد",
                "سنة صنع السفينة" to "2018",
                "نوع الإثبات" to "شهادة بناء",
                "حوض البناء" to "Hyundai Shipyard",
                "تاريخ بدء البناء" to "2014-03-01",
                "تاريخ انتهاء البناء" to "2015-01-15",
                "تاريخ أول تسجيل" to "2015-02-01",
                "بلد البناء" to "سلطنة عمان"
            )*/
            navigationManager.navigate(NavRoutes.RequestDetailRoute.createRoute(
                CheckShipCondition(
//                    transactionTitle = "إصدار تصريح ملاحة للسفن و الوحدات البحرية",
//                    title = "تم رفض الطلب",
//                    referenceNumber = "007 24 7865498",
//                    description = "تم رفض طلبكم، ولمزيد من التفاصيل يرجى الاطّلاع على القسم أدناه",
//                    refuseReason = "تم رفض طلبكم بسبب وجود في  شطب على السفن مما تمنع استكمال المعاملة وفق الإجراءات القانونية المعتمدة",
//                    shipData = shipData
                    shipData = ""
                )
            ))
            return -1
        }
        return step
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

