package com.informatique.mtcit.business.transactions
import com.informatique.mtcit.R
import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.transactions.marineunit.rules.TemporaryRegistrationRules
import com.informatique.mtcit.business.transactions.shared.DocumentConfig
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.validation.rules.DateValidationRules
import com.informatique.mtcit.business.validation.rules.DimensionValidationRules
import com.informatique.mtcit.business.validation.rules.MarineUnitValidationRules
import com.informatique.mtcit.business.validation.rules.ValidationRule
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.data.repository.MarineUnitRepository
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


class RequestInspectionStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val companyRepository: CompanyRepo,
    private val validationUseCase: FormValidationUseCase,
    private val lookupRepository: LookupRepository,
    private val marineUnitRepository: MarineUnitRepository,
    private val temporaryRegistrationRules: TemporaryRegistrationRules
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

        marineUnits = marineUnitRepository.getUserMarineUnits("currentUserId")

        return mapOf(
            "marineUnits" to marineUnits, // ✅ Return actual MarineUnit objects for validation
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

        steps.add(SharedSteps.inspectionPurposeStep(
            listOf(
                "New",
                "Used - Like New",
                "Used - Good",
                "Used - Fair",
                "Used - Poor"
            )
        ))
        steps.add(SharedSteps.inspectionAuthorityStep(
            listOf(
                "New",
                "Used - Like New",
                "Used - Good",
                "Used - Fair",
                "Used - Poor"
            )
        ))

        // Review Step (shows all collected data)
        steps.add(SharedSteps.reviewStep())

        steps.add(SharedSteps.transferInspectionToClassificationStep(
            listOf(
                "New",
                "Used - Like New",
                "Used - Good",
                "Used - Fair",
                "Used - Poor"
            )
        ))



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

    override fun processStepData(step: Int, data: Map<String, String>): Int {
        println("🔄 processStepData called with: $data")

        // ✅ Update accumulated data
        accumulatedFormData.putAll(data)

        println("📦 accumulatedFormData after update: $accumulatedFormData")

        // ... rest of existing code

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

    /**
     * Validate marine unit selection using TemporaryRegistrationRules
     * Called from MarineRegistrationViewModel when user clicks "Accept & Send" on review step
     */
    suspend fun validateMarineUnitSelection(unitId: String, userId: String): ValidationResult {
        return try {
            println("🔍 TemporaryRegistrationStrategy: Validating unit $unitId using TemporaryRegistrationRules")

            // Find the selected unit
            val selectedUnit = marineUnits.firstOrNull { it.id == unitId }

            if (selectedUnit == null) {
                println("❌ Unit not found with id: $unitId")
                return ValidationResult.Error("الوحدة البحرية المختارة غير موجودة")
            }

            println("✅ Found unit: ${selectedUnit.name}, id: ${selectedUnit.id}")

            // Use TemporaryRegistrationRules to validate
            val validationResult = temporaryRegistrationRules.validateUnit(selectedUnit, userId)
            val navigationAction = temporaryRegistrationRules.getNavigationAction(validationResult)

            println("✅ Validation result: ${validationResult::class.simpleName}")
            println("✅ Navigation action: ${navigationAction::class.simpleName}")

            ValidationResult.Success(
                validationResult = validationResult,
                navigationAction = navigationAction
            )
        } catch (e: Exception) {
            println("❌ Validation error: ${e.message}")
            e.printStackTrace()
            ValidationResult.Error(e.message ?: "فشل التحقق من حالة الفحص")
        }
    }

    /**
     * NEW: Validate a NEW marine unit that doesn't exist in the database yet
     * This is used when user is adding a new marine unit during registration
     */
    suspend fun validateNewMarineUnit(newUnit: MarineUnit, userId: String): ValidationResult {
        return try {
            println("🔍 TemporaryRegistrationStrategy: Validating NEW unit ${newUnit.name} (id: ${newUnit.id})")

            // Use TemporaryRegistrationRules to validate the new unit
            val validationResult = temporaryRegistrationRules.validateUnit(newUnit, userId)
            val navigationAction = temporaryRegistrationRules.getNavigationAction(validationResult)

            println("✅ Validation result: ${validationResult::class.simpleName}")
            println("✅ Navigation action: ${navigationAction::class.simpleName}")

            ValidationResult.Success(
                validationResult = validationResult,
                navigationAction = navigationAction
            )
        } catch (e: Exception) {
            println("❌ Validation error: ${e.message}")
            e.printStackTrace()
            ValidationResult.Error(e.message ?: "فشل التحقق من حالة الفحص")
        }
    }
}
