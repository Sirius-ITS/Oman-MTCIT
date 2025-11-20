package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.R
import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.DocumentConfig
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.SharedSteps
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

class TemporaryRegistrationStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val companyRepository: CompanyRepo,
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository
) : TransactionStrategy {

    private var portOptions: List<String> = emptyList()
    private var countryOptions: List<String> = emptyList()
    private var shipTypeOptions: List<String> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()
    private var commercialOptions: List<SelectableItem> = emptyList()
    private var typeOptions: List<PersonType> = emptyList()
    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf()

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val shipTypes = lookupRepository.getShipTypes().getOrNull() ?: emptyList()
        val commercialRegistrations = lookupRepository.getCommercialRegistrations().getOrNull() ?: emptyList()
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()

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
                isOwned = false,
                totalLength = "45 متر",
                lengthBetweenPerpendiculars = "40 متر",
                totalWidth = "12 متر",
                draft = "4 أمتار",
                height = "15 متر",
                numberOfDecks = "2",
                totalCapacity = "500 طن",
                containerCapacity = "-",
                violationsCount = "0",
                detentionsCount = "0",
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
                isOwned = true,
                totalLength = "240 متر",
                lengthBetweenPerpendiculars = "210 متر",
                totalWidth = "33 متر",
                draft = "10 أمتار",
                height = "45 متر",
                numberOfDecks = "9",
                totalCapacity = "50000 طن",
                containerCapacity = "4500 حاوية",
                violationsCount = "2",
                detentionsCount = "1",
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
                isOwned = true,
                totalLength = "180 متر",
                lengthBetweenPerpendiculars = "165 متر",
                totalWidth = "28 متر",
                draft = "12 أمتار",
                height = "38 متر",
                numberOfDecks = "7",
                totalCapacity = "75000 طن",
                containerCapacity = "-",
                violationsCount = "3",
                detentionsCount = "0",
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
                totalLength = "120 متر",
                lengthBetweenPerpendiculars = "105 متر",
                totalWidth = "22 متر",
                draft = "6 أمتار",
                height = "30 متر",
                numberOfDecks = "8",
                totalCapacity = "3000 طن",
                containerCapacity = "-",
                violationsCount = "0",
                detentionsCount = "0",
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
                totalLength = "35 متر",
                lengthBetweenPerpendiculars = "30 متر",
                totalWidth = "8 متر",
                draft = "3 أمتار",
                height = "25 متر",
                numberOfDecks = "1",
                totalCapacity = "150 طن",
                containerCapacity = "-",
                violationsCount = "0",
                detentionsCount = "0",
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
            "unitType" to shipTypes,
            "commercialRegistration" to commercialRegistrations,
            "personType" to personTypes
        )
    }

    override fun getSteps(): List<StepData> {
        val steps = mutableListOf<StepData>()

        // Step 1: Person Type
        steps.add(SharedSteps.personTypeStep(typeOptions))

        // Step 2: Commercial Registration (فقط للشركات)
        val selectedPersonType = accumulatedFormData["selectionPersonType"]
        if (selectedPersonType == "شركة") {
            steps.add(SharedSteps.commercialRegistrationStep(commercialOptions))
        }

        // Step 3: Marine Unit Selection
        steps.add(
            SharedSteps.marineUnitSelectionStep(
                units = marineUnits,
                allowMultipleSelection = false,
                showAddNewButton = true,
                showOwnedUnitsWarning = true
            )
        )

        // ✅ التحقق الصحيح من اختيار المستخدم
        val isAddingNewUnitFlag = accumulatedFormData["isAddingNewUnit"]?.toBoolean() ?: false
        val selectedUnitsJson = accumulatedFormData["selectedMarineUnits"]

        // ✅ المستخدم اختار سفينة موجودة إذا كان فيه JSON مش فاضي ومش "[]"
        val hasSelectedExistingUnit = !selectedUnitsJson.isNullOrEmpty() &&
                selectedUnitsJson != "[]"

        // ✅ WORKAROUND: لو selectedMarineUnits موجود وفاضي "[]" ومفيش isAddingNewUnit flag
        // معناها المستخدم ضغط على الزرار بس الفلاج مبعتش صح
        val isAddingNewUnit = isAddingNewUnitFlag ||
                (selectedUnitsJson == "[]" && accumulatedFormData.containsKey("selectedMarineUnits"))

        // ✅ طباعة للتتبع (Debug)
        println("🔍 DEBUG - isAddingNewUnitFlag: $isAddingNewUnitFlag")
        println("🔍 DEBUG - selectedUnitsJson: $selectedUnitsJson")
        println("🔍 DEBUG - accumulatedFormData: $accumulatedFormData")
        println("🔍 DEBUG - hasSelectedExistingUnit: $hasSelectedExistingUnit")
        println("🔍 DEBUG - isAddingNewUnit (final): $isAddingNewUnit")
        println("🔍 DEBUG - Will show new unit steps: ${isAddingNewUnit && !hasSelectedExistingUnit}")

        // ✅ نضيف steps الإضافة فقط لو المستخدم ضغط "إضافة جديدة" ومش مختار سفينة موجودة
        if (isAddingNewUnit && !hasSelectedExistingUnit) {
            println("✅ Adding new unit steps")

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
        } else {
            println("❌ NOT adding new unit steps - isAddingNewUnit: $isAddingNewUnit, hasSelectedExistingUnit: $hasSelectedExistingUnit")
        }

        // Review Step
        steps.add(SharedSteps.reviewStep())
        steps.add(
            SharedSteps.marineUnitNameSelectionStep(
                showReservationInfo = true
            )
        )
        println("📋 Total steps count: ${steps.size}")
        return steps
    }

    override fun validateStep(step: Int, data: Map<String, Any>): Pair<Boolean, Map<String, String>> {
        val stepData = getSteps().getOrNull(step) ?: return Pair(false, emptyMap())
        val formData = data.mapValues { it.value.toString() }
        return validationUseCase.validateStep(stepData, formData)
    }

    override fun processStepData(step: Int, data: Map<String, String>): Int {
        println("🔄 processStepData called with: $data")

        // ✅ تحديث الـ accumulatedFormData
        accumulatedFormData.putAll(data)

        println("📦 accumulatedFormData after update: $accumulatedFormData")

        // ✅ لو المستخدم غيّر اختياره في Marine Unit Selection Step
        if (data.containsKey("selectedMarineUnits") || data.containsKey("isAddingNewUnit")) {
            println("🔀 Marine unit selection changed")
            handleMarineUnitSelectionChange(data)
        }

        return step
    }

    // ✅ دالة جديدة لمعالجة تغيير اختيار السفينة
    private fun handleMarineUnitSelectionChange(data: Map<String, String>) {
        val isAddingNew = data["isAddingNewUnit"]?.toBoolean() ?: false
        val hasSelectedUnit = !data["selectedMarineUnits"].isNullOrEmpty() &&
                data["selectedMarineUnits"] != "[]"

        println("🔧 handleMarineUnitSelectionChange - isAddingNew: $isAddingNew, hasSelectedUnit: $hasSelectedUnit")

        if (isAddingNew && hasSelectedUnit) {
            // ✅ المستخدم اختار "إضافة جديدة" بعد ما كان مختار سفينة
            println("🗑️ Removing selected units because adding new")
            accumulatedFormData.remove("selectedMarineUnits")
            resetNewUnitData()
        } else if (!isAddingNew && hasSelectedUnit) {
            // ✅ المستخدم اختار سفينة موجودة بعد ما كان في وضع "إضافة جديدة"
            println("🗑️ Resetting new unit data because selected existing unit")
            accumulatedFormData["isAddingNewUnit"] = "false"
            resetNewUnitData()
        }
    }

    // ✅ دالة لمسح بيانات السفينة الجديدة
    private fun resetNewUnitData() {
        println("🧹 Resetting new unit data")

        val keysToRemove = listOf(
            // Unit Selection Data
            "unitType",
            "unitClassification",
            "callSign",
            "imoNumber",
            "registrationPort",
            "mmsi",
            "manufacturerYear",
            "constructionpool",
            "proofType",
            "proofDocument",
            "constructionEndDate",
            "firstRegistrationDate",
            "registrationCountry",

            // Dimensions
            "overallLength",
            "overallWidth",
            "depth",
            "height",
            "decksCount",

            // Weights
            "grossTonnage",
            "netTonnage",
            "staticLoad",
            "maxPermittedLoad",

            // Engine Info
            "engines",

            // Owner Info
            "owners",
            "totalOwnersCount",

            // Documents
            "shipbuildingCertificate",
            "inspectionDocuments",

            // Unit Name
            "marineUnitName",

            // Insurance
            "insuranceDocumentNumber",
            "insuranceCountry",
            "insuranceCompany",
            "insuranceDocumentFile"
        )

        keysToRemove.forEach { key ->
            if (accumulatedFormData.containsKey(key)) {
                println("  Removing key: $key")
            }
            accumulatedFormData.remove(key)
        }
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