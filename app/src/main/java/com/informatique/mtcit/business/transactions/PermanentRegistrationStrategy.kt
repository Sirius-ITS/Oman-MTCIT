package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.R
import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.transactions.shared.DocumentConfig
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
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
 * Strategy for Permanent Registration Certificate
 * DEMONSTRATION: Adds an extra "Previous Registration" step to show dynamic step addition
 */
class PermanentRegistrationStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val companyRepository: CompanyRepo,
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository
) : TransactionStrategy {

    // Cache for loaded dropdown options
    private var portOptions: List<String> = emptyList()
    private var countryOptions: List<String> = emptyList()
    private var shipTypeOptions: List<String> = emptyList()
    private var typeOptions: List<PersonType> = emptyList()
    private var commercialOptions: List<SelectableItem> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()


    // ✅ الحل: اعمل cache للـ form data
    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf()


    override suspend fun loadDynamicOptions(): Map<String, List<String>> {
        // Load all dropdown options from API
        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val shipTypes = lookupRepository.getShipTypes().getOrNull() ?: emptyList()
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()
        val commercialRegistrations = lookupRepository.getCommercialRegistrations().getOrNull() ?: emptyList()

        portOptions = ports
        countryOptions = countries
        shipTypeOptions = shipTypes
        typeOptions = personTypes
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
                isOwned = false,
                // الأبعاد
                totalLength = "45 متر",
                lengthBetweenPerpendiculars = "40 متر",
                totalWidth = "12 متر",
                draft = "4 أمتار",
                height = "15 متر",
                numberOfDecks = "2",
                // السعة والحمولة
                totalCapacity = "500 طن",
                containerCapacity = "-",
                // المخالفات والاحتجازات
                violationsCount = "0",
                detentionsCount = "0",
                // الديون والمستحقات
                amountDue = "0 ريال",
                paymentStatus = "مسدد"
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
                isOwned = true, // ⚠️ مملوكة - هتظهر مع التحذير
                // الأبعاد
                totalLength = "240 متر",
                lengthBetweenPerpendiculars = "210 متر",
                totalWidth = "33 متر",
                draft = "10 أمتار",
                height = "45 متر",
                numberOfDecks = "9",
                // السعة والحمولة
                totalCapacity = "50000 طن",
                containerCapacity = "4500 حاوية",
                // المخالفات والاحتجازات
                violationsCount = "2",
                detentionsCount = "1",
                // الديون والمستحقات
                amountDue = "15000 ريال",
                paymentStatus = "مستحق"
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
                isOwned = true, // ⚠️ مملوكة
                // الأبعاد
                totalLength = "180 متر",
                lengthBetweenPerpendiculars = "165 متر",
                totalWidth = "28 متر",
                draft = "12 أمتار",
                height = "38 متر",
                numberOfDecks = "7",
                // السعة والحمولة
                totalCapacity = "75000 طن",
                containerCapacity = "-",
                // المخالفات والاحتجازات
                violationsCount = "3",
                detentionsCount = "0",
                // الديون والمستحقات
                amountDue = "8500 ريال",
                paymentStatus = "تحت المراجعة"
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
                isOwned = false,
                // الأبعاد
                totalLength = "120 متر",
                lengthBetweenPerpendiculars = "105 متر",
                totalWidth = "22 متر",
                draft = "6 أمتار",
                height = "30 متر",
                numberOfDecks = "8",
                // السعة والحمولة
                totalCapacity = "3000 طن",
                containerCapacity = "-",
                // المخالفات والاحتجازات
                violationsCount = "0",
                detentionsCount = "0",
                // الديون والمستحقات
                amountDue = "0 ريال",
                paymentStatus = "مسدد"
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
                isOwned = false,
                // الأبعاد
                totalLength = "35 متر",
                lengthBetweenPerpendiculars = "30 متر",
                totalWidth = "8 متر",
                draft = "3 أمتار",
                height = "25 متر",
                numberOfDecks = "1",
                // السعة والحمولة
                totalCapacity = "150 طن",
                containerCapacity = "-",
                // المخالفات والاحتجازات
                violationsCount = "0",
                detentionsCount = "0",
                // الديون والمستحقات
                amountDue = "0 ريال",
                paymentStatus = "مسدد"
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
        val steps = mutableListOf<StepData>()

        steps.add(SharedSteps.personTypeStep(typeOptions))

        // Step 1: Commercial Registration (بس لو اختار شركة)
        val selectedPersonType = accumulatedFormData["selectionPersonType"]

        if (selectedPersonType == "شركة") {  // ⚠️ بيقارن بالـ string "شركة"
            steps.add(SharedSteps.commercialRegistrationStep(commercialOptions))
        }

        steps.add(
            SharedSteps.marineUnitRegistrationCertificateStep(
                showInfoMessage = true
            )
        )
        val hasTemporaryCertificate = accumulatedFormData["hasTemporaryRegistrationCertificate"]

        if (hasTemporaryCertificate == "yes") {
            // ✅ لو "نعم": اتخطى كل الـ steps التقنية وروح على Insurance مباشرة
            println("✅ User has temporary certificate - Skipping technical steps")
            // بس هنضيف Owner Info (مهم للتسجيل)


            steps.add(
                SharedSteps.marineUnitSelectionStep(
                    units = marineUnits,
                    allowMultipleSelection = false, // اختيار وحدة واحدة فقط
                    showAddNewButton = false,
                    showOwnedUnitsWarning = true
                )
            )
            steps.add(
                SharedSteps.insuranceDocumentStep(
                    countries = countryOptions
                )
            )
            steps.add(
                SharedSteps.marineUnitNameSelectionStep(
                    showReservationInfo = true
                )
            )

        } else if (hasTemporaryCertificate == "no") {

//            // ✅ نشيك لو المستخدم ضاف سفينة جديدة او اختار سفينة موجودة
//            val isAddingNewUnit = accumulatedFormData["isAddingNewUnit"] == "true"
//            val selectedUnitsJson = accumulatedFormData["selectedMarineUnits"] ?: "[]"
//            val selectedUnits = try {
//                kotlinx.serialization.json.Json.decodeFromString<List<String>>(selectedUnitsJson)
//            } catch (_: Exception) {
//                emptyList()
//            }
//
//
//            if (isAddingNewUnit || selectedUnits.isEmpty()) {
            steps.add(
                SharedSteps.unitSelectionStep(
                    shipTypes = shipTypeOptions,
                    ports = portOptions,
                    countries = countryOptions,
                    includeIMO = true,
                    includeMMSI = true,
                    includeManufacturer = true,
                    maritimeactivity = shipTypeOptions,
                    includeProofDocument = false,
                    includeConstructionDates = true,
                    includeRegistrationCountry = true
                )
            )

            steps.add(
                SharedSteps.marineUnitDimensionsStep(
                    includeHeight = true,
                    includeDecksCount = true
                )
            )

            steps.add(
                SharedSteps.marineUnitWeightsStep(
                    includeMaxPermittedLoad = true
                )
            )

            steps.add(
                SharedSteps.engineInfoStep(
                    manufacturers = listOf(
                        "Manufacturer 1",
                        "Manufacturer 2",
                        "Manufacturer 3"
                    ),
                    countries = countryOptions,
                    fuelTypes = listOf("Gas 80", "Gas 90", "Gas 95", "Diesel", "Electric"),
                    engineConditions = listOf(
                        "New",
                        "Used - Like New",
                        "Used - Good",
                        "Used - Fair",
                        "Used - Poor"
                    ),
                )
            )
//            }


            steps.add(
                SharedSteps.ownerInfoStep(
                    nationalities = countryOptions,
                    countries = countryOptions,
                    includeCompanyFields = true,
                )
            )
            steps.add(
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
                )
            )
            steps.add(SharedSteps.reviewStep())
            steps.add(
                SharedSteps.insuranceDocumentStep(
                    countries = countryOptions
                )
            )
        }
            steps.add(
                SharedSteps.marineUnitNameSelectionStep(
                    showReservationInfo = true
                )
            )

        return steps
    }

    override fun validateStep(
        step: Int,
        data: Map<String, Any>
    ): Pair<Boolean, Map<String, String>> {
        val stepData = getSteps().getOrNull(step) ?: return Pair(false, emptyMap())
        val formData = data.mapValues { it.value.toString() }
        return validationUseCase.validateStep(stepData, formData)
    }

    override fun processStepData(step: Int, data: Map<String, String>): Int {
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("💾 Processing Step $step Data: $data")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // ✅ احفظ الـ data في الـ cache
        accumulatedFormData.putAll(data)

        println("📦 Accumulated Data After Update: $accumulatedFormData")

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

