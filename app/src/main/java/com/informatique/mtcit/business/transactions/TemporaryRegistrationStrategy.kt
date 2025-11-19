package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.R
import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.transactions.shared.DocumentConfig
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.validation.rules.DateValidationRules
import com.informatique.mtcit.business.validation.rules.DimensionValidationRules
import com.informatique.mtcit.business.validation.rules.MarineUnitValidationRules
import com.informatique.mtcit.business.validation.rules.ValidationRule
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

        // ✅ Get validation rules for this step
        val rules = getValidationRulesForStep(step, stepData)

        // ✅ Use accumulated data for validation (enables cross-step validation)
        return validationUseCase.validateStepWithAccumulatedData(
            stepData = stepData,
            currentStepData = formData,
            allAccumulatedData = accumulatedFormData,
            crossFieldRules = rules
        )
    }

    /**
     * Get validation rules based on step content
     */
    private fun getValidationRulesForStep(stepIndex: Int, stepData: StepData): List<ValidationRule> {
        val fieldIds = stepData.fields.map { it.id }
        val rules = mutableListOf<ValidationRule>()

        if (fieldIds.contains("grossTonnage")) {
            println("🔍 Step contains grossTonnage field")


            // ✅ Marine Unit Weights Step - Always add cross-step rules
            if (fieldIds.contains("grossTonnage")) {


                println("🔍 Step contains grossTonnage field")


                // ✅ Pass accumulated data to validation rules
                rules.addAll(MarineUnitValidationRules.getAllWeightRules(accumulatedFormData))
                println("🔍 Added ${rules.size} marine unit validation rules")
            }

            // Check if MMSI field exists
            if (accumulatedFormData.containsKey("mmsi")) {
                println("🔍 ✅ Adding MMSI validation rule")
                rules.add(MarineUnitValidationRules.mmsiRequiredForMediumVessels(accumulatedFormData ))
            }
        }

        // Same-step validations
        if (fieldIds.containsAll(listOf("grossTonnage", "netTonnage"))) {
            rules.add(MarineUnitValidationRules.netTonnageLessThanOrEqualGross())
        }

        if (fieldIds.containsAll(listOf("grossTonnage", "staticLoad"))) {
            rules.add(MarineUnitValidationRules.staticLoadValidation())
        }

        if (fieldIds.containsAll(listOf("staticLoad", "maxPermittedLoad"))) {
            rules.add(MarineUnitValidationRules.maxPermittedLoadValidation())
        }

        // Dimension Rules
        if (fieldIds.containsAll(listOf("overallLength", "overallWidth"))) {
            rules.add(DimensionValidationRules.lengthGreaterThanWidth())
        }

        if (fieldIds.containsAll(listOf("height", "grossTonnage"))) {
            rules.add(DimensionValidationRules.heightValidation())
        }

        if (fieldIds.containsAll(listOf("decksCount", "grossTonnage"))) {
            rules.add(DimensionValidationRules.deckCountValidation())
        }

        // Date Rules
        if (fieldIds.contains("manufacturerYear")) {
            rules.add(DateValidationRules.manufacturerYearValidation())
        }

        if (fieldIds.containsAll(listOf("constructionEndDate", "firstRegistrationDate"))) {
            rules.add(DateValidationRules.registrationAfterConstruction())
        }

        return rules
    }

    override fun processStepData(step: Int, data: Map<String, String>): Map<String, String> {
        println("🔄 processStepData called with: $data")

        // ✅ Update accumulated data
        accumulatedFormData.putAll(data)

        println("📦 accumulatedFormData after update: $accumulatedFormData")

        // ... rest of existing code

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


//package com.informatique.mtcit.business.transactions
//
//import com.informatique.mtcit.R
//import com.informatique.mtcit.business.BusinessState
//import com.informatique.mtcit.business.usecases.FormValidationUseCase
//import com.informatique.mtcit.business.transactions.shared.DocumentConfig
//import com.informatique.mtcit.business.transactions.shared.MarineUnit
//import com.informatique.mtcit.business.transactions.shared.SharedSteps
//import com.informatique.mtcit.data.repository.ShipRegistrationRepository
//import com.informatique.mtcit.data.repository.LookupRepository
//import com.informatique.mtcit.ui.components.PersonType
//import com.informatique.mtcit.ui.components.SelectableItem
//import com.informatique.mtcit.ui.repo.CompanyRepo
//import com.informatique.mtcit.ui.viewmodels.StepData
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.catch
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.flow.flowOn
//import javax.inject.Inject
//import com.informatique.mtcit.business.validation.rules.*
//import com.informatique.mtcit.common.FormField
//
//
//class TemporaryRegistrationStrategy @Inject constructor(
//    private val repository: ShipRegistrationRepository,
//    private val companyRepository: CompanyRepo,
//    private val validationUseCase: FormValidationUseCase,
//    private val lookupRepository: LookupRepository
//) : TransactionStrategy {
//
//    private var portOptions: List<String> = emptyList()
//    private var countryOptions: List<String> = emptyList()
//    private var shipTypeOptions: List<String> = emptyList()
//    private var marineUnits: List<MarineUnit> = emptyList()
//    private var commercialOptions: List<SelectableItem> = emptyList()
//    private var typeOptions: List<PersonType> = emptyList()
//    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf()
//
//    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
//        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
//        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
//        val shipTypes = lookupRepository.getShipTypes().getOrNull() ?: emptyList()
//        val commercialRegistrations = lookupRepository.getCommercialRegistrations().getOrNull() ?: emptyList()
//        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()
//
//        portOptions = ports
//        countryOptions = countries
//        shipTypeOptions = shipTypes
//        commercialOptions = commercialRegistrations
//        typeOptions = personTypes
//
//        marineUnits = listOf(
//            MarineUnit(
//                id = "1",
//                name = "الريادة البحرية",
//                type = "سفينة صيد",
//                imoNumber = "9990001",
//                callSign = "A9BC2",
//                maritimeId = "470123456",
//                registrationPort = "صحار",
//                activity = "صيد",
//                isOwned = false,
//                totalLength = "45 متر",
//                lengthBetweenPerpendiculars = "40 متر",
//                totalWidth = "12 متر",
//                draft = "4 أمتار",
//                height = "15 متر",
//                numberOfDecks = "2",
//                totalCapacity = "500 طن",
//                containerCapacity = "-",
//                violationsCount = "0",
//                detentionsCount = "0",
//                amountDue = "0 ريال",
//                paymentStatus = "مسدد"
//            ),
//            MarineUnit(
//                id = "3",
//                name = "النجم الساطع",
//                type = "سفينة شحن",
//                imoNumber = "9990002",
//                callSign = "B8CD3",
//                maritimeId = "470123457",
//                registrationPort = "مسقط",
//                activity = "شحن دولي",
//                isOwned = true,
//                totalLength = "240 متر",
//                lengthBetweenPerpendiculars = "210 متر",
//                totalWidth = "33 متر",
//                draft = "10 أمتار",
//                height = "45 متر",
//                numberOfDecks = "9",
//                totalCapacity = "50000 طن",
//                containerCapacity = "4500 حاوية",
//                violationsCount = "2",
//                detentionsCount = "1",
//                amountDue = "15000 ريال",
//                paymentStatus = "مستحق"
//            ),
//            MarineUnit(
//                id = "8",
//                name = "البحر الهادئ",
//                type = "سفينة صهريج",
//                imoNumber = "9990008",
//                callSign = "H8IJ9",
//                maritimeId = "470123463",
//                registrationPort = "صلالة",
//                activity = "نقل وقود",
//                isOwned = true,
//                totalLength = "180 متر",
//                lengthBetweenPerpendiculars = "165 متر",
//                totalWidth = "28 متر",
//                draft = "12 أمتار",
//                height = "38 متر",
//                numberOfDecks = "7",
//                totalCapacity = "75000 طن",
//                containerCapacity = "-",
//                violationsCount = "3",
//                detentionsCount = "0",
//                amountDue = "8500 ريال",
//                paymentStatus = "تحت المراجعة"
//            ),
//            MarineUnit(
//                id = "9",
//                name = "اللؤلؤة البيضاء",
//                type = "سفينة سياحية",
//                imoNumber = "9990009",
//                callSign = "I9JK0",
//                maritimeId = "470123464",
//                registrationPort = "مسقط",
//                activity = "رحلات سياحية",
//                isOwned = false,
//                totalLength = "120 متر",
//                lengthBetweenPerpendiculars = "105 متر",
//                totalWidth = "22 متر",
//                draft = "6 أمتار",
//                height = "30 متر",
//                numberOfDecks = "8",
//                totalCapacity = "3000 طن",
//                containerCapacity = "-",
//                violationsCount = "0",
//                detentionsCount = "0",
//                amountDue = "0 ريال",
//                paymentStatus = "مسدد"
//            ),
//            MarineUnit(
//                id = "10",
//                name = "الشراع الذهبي",
//                type = "سفينة شراعية",
//                imoNumber = "9990010",
//                callSign = "J0KL1",
//                maritimeId = "470123465",
//                registrationPort = "صحار",
//                activity = "تدريب بحري",
//                isOwned = false,
//                totalLength = "35 متر",
//                lengthBetweenPerpendiculars = "30 متر",
//                totalWidth = "8 متر",
//                draft = "3 أمتار",
//                height = "25 متر",
//                numberOfDecks = "1",
//                totalCapacity = "150 طن",
//                containerCapacity = "-",
//                violationsCount = "0",
//                detentionsCount = "0",
//                amountDue = "0 ريال",
//                paymentStatus = "مسدد"
//            )
//        )
//
//        return mapOf(
//            "marineUnits" to marineUnits.map { it.maritimeId },
//            "registrationPort" to ports,
//            "ownerNationality" to countries,
//            "ownerCountry" to countries,
//            "registrationCountry" to countries,
//            "unitType" to shipTypes,
//            "commercialRegistration" to commercialRegistrations,
//            "personType" to personTypes
//        )
//    }
//
//    override fun getSteps(): List<StepData> {
//        val steps = mutableListOf<StepData>()
//
//        // Step 1: Person Type
//        steps.add(SharedSteps.personTypeStep(typeOptions))
//
//        // Step 2: Commercial Registration (فقط للشركات)
//        val selectedPersonType = accumulatedFormData["selectionPersonType"]
//        if (selectedPersonType == "شركة") {
//            steps.add(SharedSteps.commercialRegistrationStep(commercialOptions))
//        }
//
//        // Step 3: Marine Unit Selection
//        steps.add(
//            SharedSteps.marineUnitSelectionStep(
//                units = marineUnits,
//                allowMultipleSelection = false,
//                showAddNewButton = true,
//                showOwnedUnitsWarning = true
//            )
//        )
//
//        val isAddingNewUnitFlag = accumulatedFormData["isAddingNewUnit"]?.toBoolean() ?: false
//        val selectedUnitsJson = accumulatedFormData["selectedMarineUnits"]
//
//        val hasSelectedExistingUnit = !selectedUnitsJson.isNullOrEmpty() &&
//                selectedUnitsJson != "[]"
//
//        val isAddingNewUnit = isAddingNewUnitFlag ||
//                (selectedUnitsJson == "[]" && accumulatedFormData.containsKey("selectedMarineUnits"))
//
//        println("🔍 DEBUG - isAddingNewUnitFlag: $isAddingNewUnitFlag")
//        println("🔍 DEBUG - selectedUnitsJson: $selectedUnitsJson")
//        println("🔍 DEBUG - hasSelectedExistingUnit: $hasSelectedExistingUnit")
//        println("🔍 DEBUG - isAddingNewUnit (final): $isAddingNewUnit")
//
//        if (isAddingNewUnit && !hasSelectedExistingUnit) {
//            println("✅ Adding new unit steps")
//            // ✅ STEP 3: Unit Selection (now we know if IMO/MMSI required)
//            steps.add(
//                SharedSteps.unitSelectionStep(
//                    shipTypes = shipTypeOptions,
//                    ports = portOptions,
//                    countries = countryOptions,
//                    includeIMO = true,
//                    includeMMSI = true,
//                    includeManufacturer = true,
//                    maritimeactivity = shipTypeOptions,
//                    includeProofDocument = false,
//                    includeConstructionDates = true,
//                    includeRegistrationCountry = true
//                )
//            )
//
//
//            // ✅ STEP 2: Dimensions
//            steps.add(
//                SharedSteps.marineUnitDimensionsStep(
//                    includeHeight = true,
//                    includeDecksCount = true
//                )
//            )
//// ✅ STEP 1: Weights FIRST (to determine if IMO/MMSI needed)
//            steps.add(
//                SharedSteps.marineUnitWeightsStep(
//                    includeMaxPermittedLoad = true
//                )
//            )
//
//
//            steps.add(
//                SharedSteps.engineInfoStep(
//                    manufacturers = listOf(
//                        "Manufacturer 1",
//                        "Manufacturer 2",
//                        "Manufacturer 3"
//                    ),
//                    countries = countryOptions,
//                    fuelTypes = listOf("Gas 80", "Gas 90", "Gas 95", "Diesel", "Electric"),
//                    engineConditions = listOf(
//                        "New",
//                        "Used - Like New",
//                        "Used - Good",
//                        "Used - Fair",
//                        "Used - Poor"
//                    ),
//                )
//            )
//
//            steps.add(
//                SharedSteps.ownerInfoStep(
//                    nationalities = countryOptions,
//                    countries = countryOptions,
//                    includeCompanyFields = true,
//                )
//            )
//
//            steps.add(
//                SharedSteps.documentsStep(
//                    requiredDocuments = listOf(
//                        DocumentConfig(
//                            id = "shipbuildingCertificate",
//                            labelRes = R.string.shipbuilding_certificate_or_sale_contract,
//                            mandatory = true
//                        ),
//                        DocumentConfig(
//                            id = "inspectionDocuments",
//                            labelRes = R.string.inspection_documents,
//                            mandatory = true
//                        )
//                    )
//                )
//            )
//        }
//
//        steps.add(SharedSteps.reviewStep())
//        steps.add(
//            SharedSteps.marineUnitNameSelectionStep(
//                showReservationInfo = true
//            )
//        )
//
//        println("📋 Total steps count: ${steps.size}")
//        return steps
//    }
//
//    override fun validateStep(step: Int, data: Map<String, Any>): Pair<Boolean, Map<String, String>> {
//        val stepData = getSteps().getOrNull(step) ?: return Pair(false, emptyMap())
//
//        // ✅ Merge current step data with accumulated data
//        val formData = data.mapValues { it.value.toString() }
//        val mergedData = accumulatedFormData.toMutableMap().apply {
//            putAll(formData)
//        }
//
//        println("🔍 VALIDATION - Step: $step")
//        println("🔍 VALIDATION - Current data: $formData")
//        println("🔍 VALIDATION - Accumulated data: $accumulatedFormData")
//        println("🔍 VALIDATION - Merged data: $mergedData")
//
//        // ✅ Get rules that apply to this step considering ALL accumulated data
//        val rules = getValidationRulesForStep(step, stepData, mergedData)
//
//        println("🔍 VALIDATION - Applicable rules count: ${rules.size}")
//        rules.forEach { rule ->
//            println("   - ${rule.javaClass.simpleName}")
//        }
//
//        // ✅ Convert merged data to FormFields for validation
//        val allFields = createFormFieldsFromData(mergedData, stepData)
//
//        // ✅ Validate each rule
//        val errors = mutableMapOf<String, String>()
//        rules.forEach { rule ->
//            val result = rule.validate(allFields)
//            if (result is ValidationResult.Invalid) {
//                println("❌ VALIDATION FAILED: ${result.error} for field ${result.fieldId}")
//                errors[result.fieldId] = result.error
//            }
//        }
//
//        val isValid = errors.isEmpty()
//        println("🔍 VALIDATION RESULT: ${if (isValid) "✅ VALID" else "❌ INVALID"}")
//
//        return Pair(isValid, errors)
//    }
//
//    /**
//     * ✅ Create FormFields from data map for validation
//     */
//    private fun createFormFieldsFromData(data: Map<String, String>, currentStepData: StepData): List<FormField> {
//        val fields = mutableListOf<FormField>()
//
//        // Add fields from current step
//        fields.addAll(currentStepData.fields)
//
//        // Add virtual fields for accumulated data that's not in current step
//        data.forEach { (key, value) ->
//            if (fields.none { it.id == key }) {
//                // Create appropriate field type based on the data
//                fields.add(
//                    FormField.TextField(
//                        id = key,
//                        labelRes = 0,
//                        value = value,
//                        mandatory = false
//                    )
//                )
//            } else {
//                // Update existing field with accumulated value
//                val index = fields.indexOfFirst { it.id == key }
//                if (index >= 0) {
//                    when (val field = fields[index]) {
//                        is FormField.TextField -> {
//                            fields[index] = field.copy(value = value)
//                        }
//                        is FormField.DropDown -> {
//                            fields[index] = field.copy(value = value)
//                        }
//                        else -> { /* Keep as is */ }
//                    }
//                }
//            }
//        }
//
//        return fields
//    }
//
//    /**
//     * ✅ Get validation rules based on step content AND accumulated data
//     */
//    private fun getValidationRulesForStep(
//        stepIndex: Int,
//        stepData: StepData,
//        allData: Map<String, String>
//    ): List<ValidationRule> {
//        val currentStepFieldIds = stepData.fields.map { it.id }
//        val allFieldIds = allData.keys.toList() + currentStepFieldIds
//        val rules = mutableListOf<ValidationRule>()
//
//        println("🔍 Getting rules for step $stepIndex")
//        println("   Current step fields: $currentStepFieldIds")
//        println("   All available fields: $allFieldIds")
//
//        // ✅ CRITICAL FIX: Check if we're on weights step AND need IMO/MMSI
//        val isWeightsStep = currentStepFieldIds.contains("grossTonnage")
//        val hasGrossTonnageData = allData["grossTonnage"]?.toDoubleOrNull() ?: 0.0
//
//        // ✅ On weights step: validate IMO/MMSI requirements immediately
//        if (isWeightsStep) {
//            println("   🎯 On weights step, checking tonnage: $hasGrossTonnageData")
//
//            if (hasGrossTonnageData > 500) {
//                println("   ⚠️ Tonnage > 500, IMO will be required on unit selection step")
//                // We'll validate this on unit selection step, but warn here
//            }
//
//            if (hasGrossTonnageData > 300) {
//                println("   ⚠️ Tonnage > 300, MMSI will be required on unit selection step")
//            }
//        }
//
//        // ✅ IMO validation - Check on BOTH unit selection step AND weights step
//        val isUnitSelectionStep = currentStepFieldIds.contains("imoNumber")
//        if (isUnitSelectionStep && allFieldIds.contains("grossTonnage")) {
//            println("   ✅ Adding IMO rule (on unit selection step with grossTonnage data)")
//            rules.add(MarineUnitValidationRules.imoRequiredForLargeVessels())
//        }
//
//        // ✅ MMSI validation
//        if (currentStepFieldIds.contains("mmsi") && allFieldIds.contains("grossTonnage")) {
//            println("   ✅ Adding MMSI rule")
//            rules.add(MarineUnitValidationRules.mmsiRequiredForMediumVessels())
//        }
//
//        // ✅ Net tonnage validation - only when BOTH are in current step
//        if (currentStepFieldIds.containsAll(listOf("grossTonnage", "netTonnage"))) {
//            println("   ✅ Adding net tonnage rule")
//            rules.add(MarineUnitValidationRules.netTonnageLessThanOrEqualGross())
//        }
//
//        // ✅ Static load validation
//        if (currentStepFieldIds.containsAll(listOf("grossTonnage", "staticLoad"))) {
//            println("   ✅ Adding static load rule")
//            rules.add(MarineUnitValidationRules.staticLoadValidation())
//        }
//
//        // ✅ Max permitted load validation
//        if (currentStepFieldIds.containsAll(listOf("staticLoad", "maxPermittedLoad"))) {
//            println("   ✅ Adding max permitted load rule")
//            rules.add(MarineUnitValidationRules.maxPermittedLoadValidation())
//        }
//
//        // ✅ Dimension Rules
//        if (currentStepFieldIds.containsAll(listOf("overallLength", "overallWidth"))) {
//            println("   ✅ Adding length/width rule")
//            rules.add(DimensionValidationRules.lengthGreaterThanWidth())
//        }
//
//        if (currentStepFieldIds.contains("height") && allFieldIds.contains("grossTonnage")) {
//            println("   ✅ Adding height validation rule")
//            rules.add(DimensionValidationRules.heightValidation())
//        }
//
//        if (currentStepFieldIds.contains("decksCount") && allFieldIds.contains("grossTonnage")) {
//            println("   ✅ Adding deck count rule")
//            rules.add(DimensionValidationRules.deckCountValidation())
//        }
//
//        // ✅ Date Rules
//        if (currentStepFieldIds.contains("manufacturerYear")) {
//            println("   ✅ Adding manufacturer year rule")
//            rules.add(DateValidationRules.manufacturerYearValidation())
//        }
//
//        if (currentStepFieldIds.containsAll(listOf("constructionEndDate", "firstRegistrationDate"))) {
//            println("   ✅ Adding registration date rule")
//            rules.add(DateValidationRules.registrationAfterConstruction())
//        }
//
//        return rules
//    }
//
//    override fun processStepData(step: Int, data: Map<String, String>): Map<String, String> {
//        println("🔄 processStepData called with: $data")
//
//        // ✅ Update accumulated data
//        accumulatedFormData.putAll(data)
//
//        println("📦 accumulatedFormData after update: $accumulatedFormData")
//
//        // ✅ Handle marine unit selection changes
//        if (data.containsKey("selectedMarineUnits") || data.containsKey("isAddingNewUnit")) {
//            println("🔀 Marine unit selection changed")
//            handleMarineUnitSelectionChange(data)
//        }
//
//        return data
//    }
//
//    private fun handleMarineUnitSelectionChange(data: Map<String, String>) {
//        val isAddingNew = data["isAddingNewUnit"]?.toBoolean() ?: false
//        val hasSelectedUnit = !data["selectedMarineUnits"].isNullOrEmpty() &&
//                data["selectedMarineUnits"] != "[]"
//
//        println("🔧 handleMarineUnitSelectionChange - isAddingNew: $isAddingNew, hasSelectedUnit: $hasSelectedUnit")
//
//        if (isAddingNew && hasSelectedUnit) {
//            println("🗑️ Removing selected units because adding new")
//            accumulatedFormData.remove("selectedMarineUnits")
//            resetNewUnitData()
//        } else if (!isAddingNew && hasSelectedUnit) {
//            println("🗑️ Resetting new unit data because selected existing unit")
//            accumulatedFormData["isAddingNewUnit"] = "false"
//            resetNewUnitData()
//        }
//    }
//
//    private fun resetNewUnitData() {
//        println("🧹 Resetting new unit data")
//
//        val keysToRemove = listOf(
//            "unitType", "unitClassification", "callSign", "imoNumber",
//            "registrationPort", "mmsi", "manufacturerYear", "constructionpool",
//            "proofType", "proofDocument", "constructionEndDate", "firstRegistrationDate",
//            "registrationCountry", "overallLength", "overallWidth", "depth",
//            "height", "decksCount", "grossTonnage", "netTonnage", "staticLoad",
//            "maxPermittedLoad", "engines", "owners", "totalOwnersCount",
//            "shipbuildingCertificate", "inspectionDocuments", "marineUnitName",
//            "insuranceDocumentNumber", "insuranceCountry", "insuranceCompany",
//            "insuranceDocumentFile"
//        )
//
//        keysToRemove.forEach { key ->
//            if (accumulatedFormData.containsKey(key)) {
//                println("  Removing key: $key")
//            }
//            accumulatedFormData.remove(key)
//        }
//    }
//
//    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
//        return repository.submitRegistration(data)
//    }
//
//    override fun handleFieldChange(fieldId: String, value: String, formData: Map<String, String>): Map<String, String> {
//        if (fieldId == "owner_type") {
//            val mutableFormData = formData.toMutableMap()
//            when (value) {
//                "فرد" -> {
//                    mutableFormData.remove("companyName")
//                    mutableFormData.remove("companyRegistrationNumber")
//                }
//            }
//            return mutableFormData
//        }
//        return formData
//    }
//
//    override suspend fun onFieldFocusLost(fieldId: String, value: String): FieldFocusResult {
//        if (fieldId == "companyRegistrationNumber") {
//            return handleCompanyRegistrationLookup(value)
//        }
//        return FieldFocusResult.NoAction
//    }
//
//    private suspend fun handleCompanyRegistrationLookup(registrationNumber: String): FieldFocusResult {
//        if (registrationNumber.isBlank()) {
//            return FieldFocusResult.Error("companyRegistrationNumber", "رقم السجل التجاري مطلوب")
//        }
//
//        if (registrationNumber.length < 3) {
//            return FieldFocusResult.Error("companyRegistrationNumber", "رقم السجل التجاري يجب أن يكون أكثر من 3 أرقام")
//        }
//
//        return try {
//            val result = companyRepository.fetchCompanyLookup(registrationNumber)
//                .flowOn(Dispatchers.IO)
//                .catch { throw Exception("حدث خطأ أثناء البحث عن الشركة: ${it.message}") }
//                .first()
//
//            when (result) {
//                is BusinessState.Success -> {
//                    val companyData = result.data.result
//                    if (companyData != null) {
//                        FieldFocusResult.UpdateFields(
//                            mapOf(
//                                "companyName" to companyData.arabicCommercialName,
//                                "companyType" to companyData.commercialRegistrationEntityType
//                            )
//                        )
//                    } else {
//                        FieldFocusResult.Error("companyRegistrationNumber", "لم يتم العثور على الشركة")
//                    }
//                }
//                is BusinessState.Error -> FieldFocusResult.Error("companyRegistrationNumber", result.message)
//                is BusinessState.Loading -> FieldFocusResult.NoAction
//            }
//        } catch (e: Exception) {
//            FieldFocusResult.Error("companyRegistrationNumber", e.message ?: "حدث خطأ غير متوقع")
//        }
//    }
//}