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
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import kotlin.collections.listOf

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
                // add other lookups if needed
            }
        }

        // Notify UI to refresh steps so dropdown picks up new data
        onStepsNeedRebuild?.invoke()
    }

    override fun getSteps(): List<StepData> {
        return listOf(
            // User type
            SharedSteps.personTypeStep(options = typeOptions),

            SharedSteps.commercialRegistrationStep(commercialOptions),

            SharedSteps.marineUnitSelectionStep(
                units = marineUnits,
                allowMultipleSelection = false, // اختيار وحدة واحدة فقط
                showOwnedUnitsWarning = true,
                showAddNewButton = false
            ),
            SharedSteps.sailingRegionsStep(
                sailingRegions = sailingRegionsOptions
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

    override suspend fun processStepData(step: Int, data: Map<String, String>): Int {
        // Update accumulated data first
        accumulatedFormData.putAll(data)

        // Helper to extract integer id from various JSON shapes
        fun extractIdFromJsonElement(el: kotlinx.serialization.json.JsonElement): Int? {
            return try {
                when (el) {
                    is kotlinx.serialization.json.JsonPrimitive -> el.content.toIntOrNull()
                    is kotlinx.serialization.json.JsonObject -> {
                        val primitiveCandidates = listOf("id", "shipInfo", "shipInfoId", "shipId")
                        primitiveCandidates.mapNotNull { key ->
                            try { el[key]?.jsonPrimitive?.content?.toIntOrNull() } catch (_: Exception) { null }
                        }.firstOrNull() ?: run {
                            val shipObj = el["ship"]
                            if (shipObj is kotlinx.serialization.json.JsonObject) {
                                shipObj["id"]?.jsonPrimitive?.content?.toIntOrNull()
                            } else null
                        }
                    }
                    else -> null
                }
            } catch (_: Exception) {
                null
            }
        }

        // If user chose person type 'فرد' skip commercial registration step
        if (step == 0 && data.filterValues { it == "فرد" }.isNotEmpty()){
            return 2
        }

        // Detect Unit Selection step (index 2 in this strategy) and create navigation license if needed
        if (step == 2) {
            val selectedJson = accumulatedFormData["selectedMarineUnits"]
            val existingRequestId = accumulatedFormData["requestId"]

            // If user selected existing ship(s) and we don't have a requestId yet -> create navigation license
            if (!selectedJson.isNullOrEmpty() && selectedJson != "[]" && existingRequestId.isNullOrEmpty()) {
                try {
                    // Try parse as simple array of ids e.g. ["45"] or [45]
                    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    val parsed = json.parseToJsonElement(selectedJson)

                    // Prefer exact ID strings coming from the selector (most UI send array of id strings)
                    var shipInfoId: Int? = null

                    if (parsed is kotlinx.serialization.json.JsonArray && parsed.isNotEmpty()) {
                        val first = parsed[0]
                        // If primitive string, use raw content and see if it matches any marineUnits.id
                        if (first is kotlinx.serialization.json.JsonPrimitive && first.isString) {
                          val idStr = first.content
                          println("🔎 selectedMarineUnits first element raw: '$idStr'")
                          // Exact match with marineUnits ids (string compare)
                          val matched = marineUnits.find { it.id == idStr }
                          if (matched != null) {
                            shipInfoId = idStr.toIntOrNull()
                            println("🔎 matched marineUnits entry id=${matched.id} name=${matched.shipName}")
                          } else {
                            // maybe the selector sends ship object stringified; try to extract any integer inside
                            shipInfoId = idStr.toIntOrNull()
                          }
                        } else {
                          // If first item is object, try extractor
                          extractIdFromJsonElement(first)?.let { shipInfoId = it }
                        }
                    } else if (parsed is kotlinx.serialization.json.JsonObject) {
                        // If object was provided directly
                        extractIdFromJsonElement(parsed)?.let { shipInfoId = it }
                    } else if (parsed is kotlinx.serialization.json.JsonPrimitive) {
                        extractIdFromJsonElement(parsed)?.let { shipInfoId = it }
                    }

                    // Build candidate ids to try (robust against different shapes)
                    val candidates = mutableListOf<Int>()
                    shipInfoId?.let { candidates.add(it) }

                    // Extract any integers from the raw selected JSON string as fallback
                    val regexInts = "\\d+".toRegex()
                    regexInts.findAll(selectedJson).mapNotNull { it.value.toIntOrNull() }.forEach { candidates.add(it) }

                    // Add numeric ids from loaded marineUnits
                    marineUnits.mapNotNull { it.id.toIntOrNull() }.forEach { if (!candidates.contains(it)) candidates.add(it) }

                    println("🔁 Candidate shipInfoIds to try: $candidates (from parsed=$shipInfoId, extracted ints, marineUnits)")

                    var created = false
                    val tried = mutableListOf<Int>()

                    for (candidate in candidates) {
                        if (created) break
                        if (tried.contains(candidate)) continue
                        tried.add(candidate)
                        println("📤 Trying createNavigationLicense with shipInfoId=$candidate")
                        val res = repository.createNavigationLicense(candidate)
                        res.fold(onSuccess = { resp ->
                            val navId = resp.data?.id
                            if (navId != null) {
                                accumulatedFormData["requestId"] = navId.toString()
                                accumulatedFormData["shipInfoId"] = candidate.toString()
                                println("✅ Navigation request created with id=$navId for shipInfoId=$candidate")
                                created = true
                            } else {
                                println("❌ Navigation API returned no id for candidate $candidate (response: $resp)")
                            }
                        }, onFailure = { ex ->
                            println("❌ createNavigationLicense failed for candidate $candidate: ${ex.message}")
                        })
                    }

                    if (!created) {
                        println("❌ All attempts failed. Candidates tried: $tried. SelectedJson: $selectedJson")
                    }
                } catch (e: Exception) {
                    println("❌ Exception while creating navigation license: ${e.message}")
                }
            }
        }

        // legacy check for special test value handling
        if (step == 2 && data.filterValues { it == "[\"470123456\"]" }.isNotEmpty()){
             // ✅ TODO: Uncomment after backend integration is complete
             // This forwards to RequestDetailScreen when compliance issues are detected
             /*
             navigationManager.navigate(NavRoutes.RequestDetailRoute.createRoute(
                 CheckShipCondition(shipData = "")
             ))
             return -1
             */
             // ✅ For now, continue normal flow
             return step
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
