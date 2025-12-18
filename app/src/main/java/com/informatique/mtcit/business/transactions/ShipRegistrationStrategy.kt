package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.R
import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.DocumentConfig
import com.informatique.mtcit.business.transactions.shared.SharedSteps
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
    private var shipCategoryOptions: List<String> = emptyList()
    private var marineActivityOptions: List<String> = emptyList()
    private var proofTypeOptions: List<String> = emptyList()
    private var engineStatusOptions: List<String> = emptyList()

    // NEW: Store filtered ship types based on selected category
    private var filteredShipTypeOptions: List<String> = emptyList()
    private var isShipTypeFiltered: Boolean = false

    override suspend fun loadDynamicOptions(): Map<String, List<String>> {
        // Load all dropdown options from API with caching
        val ports = lookupRepository.getPorts().getOrNull() ?: emptyList()
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val shipTypes = lookupRepository.getShipTypes().getOrNull() ?: emptyList()
        val shipCategories = lookupRepository.getShipCategories().getOrNull() ?: emptyList()
        val marineActivities = lookupRepository.getMarineActivities().getOrNull() ?: emptyList()
        val proofTypes = lookupRepository.getProofTypes().getOrNull() ?: emptyList()
        val engineStatuses = lookupRepository.getEngineStatuses().getOrNull() ?: emptyList()

        // Cache the options for use in getSteps()
        portOptions = ports
        countryOptions = countries
        shipTypeOptions = shipTypes
        shipCategoryOptions = shipCategories
        marineActivityOptions = marineActivities
        proofTypeOptions = proofTypes
        engineStatusOptions = engineStatuses

        return mapOf(
            "registrationPort" to ports,
            "ownerNationality" to countries,
            "ownerCountry" to countries,
            "registrationCountry" to countries,
            "unitType" to shipTypes,
            "unitClassification" to shipCategories,
            "maritimeActivity" to marineActivities,
            "proofType" to proofTypes,
            "engineStatus" to engineStatuses
        )
    }

    override fun getSteps(): List<StepData> {
        // Use filtered ship types if available, otherwise use all ship types
        val shipTypesToUse = if (isShipTypeFiltered) filteredShipTypeOptions else emptyList()
        val isShipTypeEnabled = isShipTypeFiltered && filteredShipTypeOptions.isNotEmpty()

        println("🔧 getSteps called - isFiltered: $isShipTypeFiltered, types count: ${shipTypesToUse.size}, enabled: $isShipTypeEnabled")
        println("🔧 filteredShipTypeOptions: $filteredShipTypeOptions")
        println("🔧 shipTypesToUse: $shipTypesToUse")

        return listOf(
            // ✅ Using SharedSteps.unitSelectionStep with filtered ship types
            SharedSteps.unitSelectionStep(
                shipTypes = shipTypesToUse,  // Use filtered types or empty list
                shipCategories = shipCategoryOptions,
                ports = portOptions,
                countries = countryOptions,
                marineActivities = marineActivityOptions,
                proofTypes = proofTypeOptions,
                buildingMaterials = emptyList(),
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
                includeCompanyFields = true
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

    override suspend fun processStepData(step: Int, data: Map<String, String>): Int {
        // No additional processing needed for ship registration
        return step
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        return repository.submitRegistration(data)
    }

    override fun handleFieldChange(fieldId: String, value: String, formData: Map<String, String>): Map<String, String> {
        val mutableFormData = formData.toMutableMap()

        // NEW: Handle ship category change - fetch filtered ship types
        if (fieldId == "unitClassification" && value.isNotBlank()) {
            println("🚢 Ship category changed to: $value")

            // Get category ID from category name
            val categoryId = lookupRepository.getShipCategoryId(value)

            if (categoryId != null) {
                println("🔍 Found category ID: $categoryId")

                // Fetch filtered ship types in a blocking manner (using runBlocking is acceptable here for UI updates)
                kotlinx.coroutines.runBlocking {
                    val filteredTypes = lookupRepository.getShipTypesByCategory(categoryId).getOrNull()
                    if (filteredTypes != null && filteredTypes.isNotEmpty()) {
                        println("✅ Loaded ${filteredTypes.size} ship types for category $categoryId")
                        filteredShipTypeOptions = filteredTypes
                        isShipTypeFiltered = true

                        // Clear the unitType field since the options changed
                        mutableFormData.remove("unitType")

                        // Add a flag to trigger step refresh
                        mutableFormData["_triggerRefresh"] = "true"
                    } else {
                        println("⚠️ No ship types found for category $categoryId")
                        filteredShipTypeOptions = emptyList()
                        isShipTypeFiltered = true
                        mutableFormData.remove("unitType")
                        mutableFormData["_triggerRefresh"] = "true"
                    }
                }
            } else {
                println("❌ Could not find category ID for: $value")
            }

            return mutableFormData
        }

        // Handle owner type change
        if (fieldId == "owner_type") {
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

    override fun getContext(): TransactionContext {
        TODO("Not yet implemented")
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
