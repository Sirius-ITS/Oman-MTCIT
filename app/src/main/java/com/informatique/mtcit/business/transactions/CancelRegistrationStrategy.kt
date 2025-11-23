package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.R
import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.MarineUnit
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
 * Strategy for Cancel Permanent Registration (Deletion/Removal)
 * DEMONSTRATION: Highly simplified - minimal fields, just cancellation info
 * Shows extreme case of field removal for streamlined process
 */
class CancelRegistrationStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val companyRepository: CompanyRepo,
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository
) : TransactionStrategy {

    private var portOptions: List<String> = emptyList()
    private var countryOptions: List<String> = emptyList()
    private var shipTypeOptions: List<String> = emptyList()
    private var commercialOptions: List<SelectableItem> = emptyList()
    private var typeOptions: List<PersonType> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()

    override suspend fun loadDynamicOptions(): Map<String, List<String>> {
        // Load all dropdown options from API
        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val shipTypes = lookupRepository.getShipTypes().getOrNull() ?: emptyList()
        val commercialRegistrations = lookupRepository.getCommercialRegistrations().getOrNull() ?: emptyList()
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()

        // Cache the options for use in getSteps()
        portOptions = ports
        countryOptions = countries
        shipTypeOptions = shipTypes
        commercialOptions = commercialRegistrations
        typeOptions = personTypes
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
            "marineUnits" to marineUnits.map { it.maritimeId },
            "registrationPort" to ports,
            "ownerNationality" to countries,
            "ownerCountry" to countries,
            "registrationCountry" to countries,
            "unitType" to shipTypes
        )
    }

    override fun getSteps(): List<StepData> {
        return listOf(

            // Step 1: No3 El Mosta5dem
            SharedSteps.personTypeStep(typeOptions),

            // Step 2: E5tar el Sigil el togary
            SharedSteps.commercialRegistrationStep(commercialOptions),

            // Step 3: El sofon el mamloka
            SharedSteps.marineUnitSelectionStep(
                units = marineUnits,
                allowMultipleSelection = false, // اختيار وحدة واحدة فقط
                showOwnedUnitsWarning = true),

            // Step ..
            /*StepData(
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
            ),*/

            // Step 4: Sabab w el mostanadat el da3ema
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
                    FormField.FileUpload(
                        id = "reasonProofDocument",
                        labelRes = R.string.reason_proof_document,
                        mandatory = true
                    )
                )
            ),

            // Step ..
            /*SharedSteps.documentsStep(
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
            ),*/

            // Step 5: Review
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