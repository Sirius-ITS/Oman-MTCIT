package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.data.repository.MarineUnitRepository
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
 * Strategy for Temporary Registration Certificate
 * Full baseline implementation with all steps
 */
class IssueNavigationPermitStrategy @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val companyRepository: CompanyRepo,
    private val validationUseCase: FormValidationUseCase,
    private val marineUnitRepository: MarineUnitRepository,
    private val lookupRepository: LookupRepository
 ) : TransactionStrategy {

    private var countryOptions: List<String> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()

    private var commercialOptions: List<SelectableItem> = emptyList()

    private var typeOptions: List<PersonType> = emptyList()
    private var sailingRegionsOptions: List<String> = emptyList()
    private var crewJobTitles: List<String> = emptyList()

    // Cache for accumulated form data (used to decide steps like other strategies)
    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf()

    // Allow ViewModel to set a callback when steps need to be rebuilt (same pattern as other strategies)
    override var onStepsNeedRebuild: (() -> Unit)? = null

    /**
     * Load ships for the selected person type / commercial registration.
     * This mirrors the behavior in Temporary/Permanent strategies so the UI can show owned ships.
     */
    override suspend fun loadShipsForSelectedType(formData: Map<String, String>): List<MarineUnit> {
        val personType = formData["selectionPersonType"]

        // For now use ownerCivilId workaround as other strategies (API limitation)
        val (ownerCivilId, commercialRegNumber) = when (personType) {
            "فرد" -> Pair("12345678", null)
            "شركة" -> Pair("12345678", null)
            else -> Pair(null, null)
        }

        marineUnits = marineUnitRepository.loadShipsForOwner(ownerCivilId, commercialRegNumber)

        // Debug logging
        println("✅ IssueNavigationPermit - Loaded ${'$'}{marineUnits.size} ships")
        marineUnits.forEach { println("   - ${'$'}{it.shipName} (ID: ${'$'}{it.id})") }

        return marineUnits
    }

    override suspend fun clearLoadedShips() {
        marineUnits = emptyList()
    }

    override fun updateAccumulatedData(data: Map<String, String>) {
        accumulatedFormData.putAll(data)
    }

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        val countries = lookupRepository.getCountries().getOrNull() ?: emptyList()
        val commercialRegistrations = lookupRepository.getCommercialRegistrations().getOrNull() ?: emptyList()
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()

        countryOptions = countries
        commercialOptions = commercialRegistrations
        typeOptions = personTypes


        return mapOf(
            "marineUnits" to emptyList<MarineUnit>(), // ✅ Empty initially
            "registrationCountry" to countries,
            "commercialRegistration" to commercialRegistrations,
            "personType" to personTypes
        )
    }

    // Load lookups when a step is opened (lazy loading)
    override suspend fun onStepOpened(stepIndex: Int) {
        val step = getSteps().getOrNull(stepIndex) ?: return
        if (step.requiredLookups.isEmpty()) return

        step.requiredLookups.forEach { lookupKey ->
            when (lookupKey) {
                "sailingRegions" -> {
                    if (sailingRegionsOptions.isEmpty()) {
                        val areas = lookupRepository.getNavigationAreas().getOrNull() ?: emptyList()
                        sailingRegionsOptions = areas
                    }
                }
                "crewJobTitles" -> {
                    if (crewJobTitles.isEmpty()) {
                        val jobs = lookupRepository.getCrewJobTitles().getOrNull() ?: emptyList()
                        crewJobTitles = jobs
                    }
                }
                 // add other lookups if needed
            }
        }

        // Notify UI to refresh steps so dropdown picks up new data
        onStepsNeedRebuild?.invoke()
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

        steps.add(
            SharedSteps.marineUnitSelectionStep(
                units = marineUnits,
                showAddNewButton = false
            )
        )
        steps.add(SharedSteps.sailingRegionsStep(
            sailingRegions = sailingRegionsOptions
        ))
        steps.add( SharedSteps.sailorInfoStep(
            jobs = crewJobTitles
        ))

        // Review Step (shows all collected data)
        steps.add(SharedSteps.reviewStep())

        println("📋 Total steps count: ${steps.size}")
        return steps
    }

    override fun validateStep(step: Int, data: Map<String, Any>): Pair<Boolean, Map<String, String>> {
        val stepData = getSteps().getOrNull(step) ?: return Pair(false, emptyMap())
        val formData = data.mapValues { it.value.toString() }
        return validationUseCase.validateStep(stepData, formData)
    }

    override suspend fun processStepData(step: Int, data: Map<String, String>): Int {
        // Update accumulated data first
        accumulatedFormData.putAll(data)

        // ✅ Check if we're on the marine unit selection step
        val currentStepData = getSteps().getOrNull(step)
        val currentStepFields = currentStepData?.fields?.map { it.id } ?: emptyList()

        if (currentStepFields.contains("selectedMarineUnits")) {
            println("🚢 Marine unit selection step completed")

            // Extract selected ship info ID
            val selectedUnitsJson = accumulatedFormData["selectedMarineUnits"]
            if (!selectedUnitsJson.isNullOrBlank() && selectedUnitsJson != "[]") {
                try {
                    // Parse selected maritime IDs
                    val cleanJson = selectedUnitsJson.trim().removeSurrounding("[", "]")
                    val maritimeIds = cleanJson.split(",").map { it.trim().removeSurrounding("\"") }
                    val firstMaritimeId = maritimeIds.firstOrNull()

                    if (!firstMaritimeId.isNullOrBlank()) {
                        // Find the ship with this maritime ID
                        val selectedShip = marineUnits.firstOrNull { it.maritimeId == firstMaritimeId }

                        if (selectedShip != null) {
                            val shipInfoId = selectedShip.id.toIntOrNull()

                            if (shipInfoId != null) {
                                println("🔵 Calling createNavigationLicense for shipInfoId=$shipInfoId")

                                // Call the API
                                val result = repository.createNavigationLicense(shipInfoId)

                                result.fold(
                                    onSuccess = { response ->
                                        println("✅ Navigation license created: requestId=${response.data?.id}")
                                        // Store the request ID for later use
                                        accumulatedFormData["navigationRequestId"] = response.data?.id?.toString() ?: ""
                                    },
                                    onFailure = { error ->
                                        println("❌ Failed to create navigation license: ${error.message}")

                                        // ✅ Parse error code from message format: "ERROR_CODE:406|message"
                                        val errorMessage = error.message ?: ""
                                        val errorCode = if (errorMessage.startsWith("ERROR_CODE:")) {
                                            val parts = errorMessage.substringAfter("ERROR_CODE:").split("|", limit = 2)
                                            if (parts.size == 2) {
                                                val code = parts[0]
                                                val message = parts[1]

                                                // Store in formData for UI to display
                                                accumulatedFormData["apiErrorCode"] = code
                                                accumulatedFormData["apiErrorMessage"] = message

                                                println("🔍 Extracted error code: $code, message: $message")
                                                code
                                            } else {
                                                null
                                            }
                                        } else {
                                            null
                                        }

                                        // If it's not a 406 error, show generic dialog instead
                                        if (errorCode != "406") {
                                            accumulatedFormData["apiError"] = errorMessage
                                        }

                                        // Return -1 to prevent navigation
                                        return -1
                                    }
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("❌ Error processing selected units: ${e.message}")
                    e.printStackTrace()
                }
            }
        }

        return step
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        return repository.submitRegistration(data)
    }

    override fun getFormData(): Map<String, String> {
        return accumulatedFormData.toMap()
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
