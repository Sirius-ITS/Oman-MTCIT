package com.informatique.mtcit.business.transactions

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.informatique.mtcit.R
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.transactions.managers.NavigationLicenseManager
import com.informatique.mtcit.business.transactions.shared.Certificate
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.business.transactions.shared.StepProcessResult
import com.informatique.mtcit.business.transactions.shared.StepType
import com.informatique.mtcit.common.ApiException
import com.informatique.mtcit.common.ErrorMessageExtractor
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.data.repository.MarineUnitRepository
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.ui.components.PersonType
import com.informatique.mtcit.ui.components.SelectableItem
import com.informatique.mtcit.ui.viewmodels.StepData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.informatique.mtcit.util.UserHelper

/**
 * Strategy for Change Captain transaction (CDD §4, requestTypeId = 10)
 *
 * Flow per CDD:
 *  1. Person Type
 *  2. [Company] Commercial registration
 *  3. Marine Unit selection → proceed-request/10 (CDD §4.1.2)
 *  4. Crew/Captain info → POST change-captain/{shipInfoId}/add-request (CDD §4.1.6)
 *  5. Affected certificates → GET certificate/{shipInfoId}/affected-certificates/10 (CDD §4.1.7)
 *  6. Review → POST general-request/10/{requestId}/send-request (CDD §4.1.9)
 *  7. Payment → GET/POST payment-receipt (CDD §4.1.11-12)
 *  8. Issue certificates → POST certificate/issue-affected-certificates/10/{requestId} (CDD §4.1.14)
 */
class ChangeCaptainNameOfShipOrUnitStrategy @Inject constructor(
    private val validationUseCase: FormValidationUseCase,
    private val marineUnitRepository: MarineUnitRepository,
    private val shipRegistrationRepository: ShipRegistrationRepository,
    private val navigationLicenseManager: NavigationLicenseManager,
    private val lookupRepository: LookupRepository,
    private val shipSelectionManager: com.informatique.mtcit.business.transactions.shared.ShipSelectionManager,
    private val reviewManager: com.informatique.mtcit.business.transactions.shared.ReviewManager,
    private val paymentManager: com.informatique.mtcit.business.transactions.shared.PaymentManager,
    @ApplicationContext private val appContext: Context
) : BaseTransactionStrategy() {

    private val requestTypeId = TransactionType.CAPTAIN_NAME_CHANGE.toRequestTypeId() // "10"
    private val transactionContext: TransactionContext = TransactionType.CAPTAIN_NAME_CHANGE.context

    private var commercialOptions: List<SelectableItem> = emptyList()
    private var typeOptions: List<PersonType> = emptyList()
    private var countryOptions: List<String> = emptyList()
    private var crewJobTitles: List<String> = emptyList()
    private var marineUnits: List<MarineUnit> = emptyList()
    private var loadedCertificates = mutableStateListOf<Certificate>()
    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf()

    // ✅ INFINITE SCROLL pagination state
    private var _currentShipsPage: Int = -1
    private var _isLastShipsPage: Boolean = true
    override val currentShipsPage: Int get() = _currentShipsPage
    override val isLastShipsPage: Boolean get() = _isLastShipsPage

    private var selectedShipInfoId: Int? = null
    private var createdRequestId: Long? = null
    private var lastApiError: String? = null
    private val apiResponses: MutableMap<String, Any> = mutableMapOf()

    // ─────────────────────────────────────────────────────────────
    // Dynamic options
    // ─────────────────────────────────────────────────────────────

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        val ownerCivilId = UserHelper.getOwnerCivilId(appContext)
        // ✅ Use raw countries with ID so nationality is stored as "id|name" (e.g. "UG|أوغندا")
        val countriesRaw = lookupRepository.getCountriesRaw()
        val commercialRegistrations = if (ownerCivilId != null)
            lookupRepository.getCommercialRegistrations(ownerCivilId).getOrNull() ?: emptyList()
        else emptyList()
        val personTypes = lookupRepository.getPersonTypes().getOrNull() ?: emptyList()
        countryOptions = countriesRaw.map { "${it.id}|${it.nameAr}" }
        commercialOptions = commercialRegistrations
        typeOptions = personTypes
        return mapOf(
            "marineUnits" to emptyList<MarineUnit>(),
            "personType" to personTypes,
            "commercialRegistration" to commercialRegistrations,
            "certificates" to loadedCertificates
        )
    }

    override suspend fun onStepOpened(stepIndex: Int) {
        val step = getSteps().getOrNull(stepIndex) ?: return

        // Load crew job titles + existing captains lazily when the crew step opens
        if (step.stepType == StepType.CREW_MANAGEMENT) {
            var needsRebuild = false

            // 1. Load job titles if not yet loaded
            if (crewJobTitles.isEmpty()) {
                val jobs = lookupRepository.getCrewJobTitlesRaw()
                crewJobTitles = jobs.map { "${it.id}|${it.nameAr}" }
                println("✅ Loaded ${crewJobTitles.size} crew job titles for Change Captain")
                needsRebuild = true
            }

            // 2. Load existing captains from API (CDD §4.1.3) if not already pre-populated
            val shipInfoId = selectedShipInfoId?.toLong()
                ?: accumulatedFormData["shipInfoId"]?.toLongOrNull()
            if (shipInfoId != null && accumulatedFormData["sailors"].isNullOrEmpty()
                || accumulatedFormData["sailors"] == "[]"
            ) {
                println("📥 Loading existing captains for shipInfoId=$shipInfoId (CDD §4.1.3)")
                navigationLicenseManager.getExistingCaptains(shipInfoId!!)
                    .onSuccess { captains ->
                        if (captains.isNotEmpty()) {
                            // Convert to SailorData JSON format that SailorList field understands
                            val sailorsJson = captains.joinToString(prefix = "[", postfix = "]") { c ->
                                val jobId = c.jobTitle.id
                                val jobNameAr = c.jobTitle.nameAr
                                val natId = c.nationality?.id ?: ""
                                val natNameAr = c.nationality?.nameAr ?: ""
                                // SailorData format: job = "ID|Name", nationality = "ID|Name", apiId = real ID
                                """{"apiId":${c.id},"nameAr":"${c.nameAr}","nameEn":"${c.nameEn}","job":"$jobId|$jobNameAr","identityNumber":"${c.civilNo ?: ""}","seamanPassportNumber":"${c.seamenBookNo}","nationality":"$natId|$natNameAr"}"""
                            }
                            accumulatedFormData["sailors"] = sailorsJson
                            println("✅ Pre-populated ${captains.size} existing captains into sailors field")
                            needsRebuild = true
                        } else {
                            println("ℹ️ No existing captains found for this ship")
                        }
                    }
                    .onFailure { e ->
                        println("⚠️ Could not load existing captains: ${e.message} — user will enter manually")
                    }
            }

            if (needsRebuild) onStepsNeedRebuild?.invoke()
        }

        // Load affected certificates when that step opens (CDD §4.1.7)
        if (step.stepType == StepType.AFFECTED_CERTIFICATES) {
            val shipInfoId = selectedShipInfoId
                ?: accumulatedFormData["shipInfoId"]?.toIntOrNull()
                ?: return
            try {
                val certs = shipRegistrationRepository.getAffectedCertificates(shipInfoId, requestTypeId.toInt())
                    .getOrNull() ?: emptyList()
                loadedCertificates.clear()
                loadedCertificates.addAll(certs)
                println("✅ Loaded ${certs.size} affected certificates for Change Captain")
                onStepsNeedRebuild?.invoke()
            } catch (e: Exception) {
                println("⚠️ Failed to load affected certificates: ${e.message}")
            }
        }

        // Load payment details when payment step opens
        if (step.stepType == StepType.PAYMENT) {
            val result = paymentManager.processStepIfNeeded(
                stepType = StepType.PAYMENT,
                formData = accumulatedFormData,
                requestTypeId = requestTypeId.toInt(),
                context = transactionContext
            )
            when (result) {
                is StepProcessResult.Success -> println("✅ Payment data loaded for Change Captain")
                is StepProcessResult.Error -> accumulatedFormData["apiError"] = result.message
                is StepProcessResult.NoAction -> {}
            }
        }

        onStepsNeedRebuild?.invoke()
    }

    // ─────────────────────────────────────────────────────────────
    // Ship loading (paginated)
    // ─────────────────────────────────────────────────────────────

    override suspend fun loadShipsForSelectedType(formData: Map<String, String>): List<MarineUnit> {
        val personType = formData["selectionPersonType"]
        val commercialReg = formData["selectionData"]
        val ownerCivilId = UserHelper.getOwnerCivilId(appContext)
        val (civilId, crNumber) = when (personType) {
            "فرد" -> Pair(ownerCivilId, null)
            "شركة" -> Pair(ownerCivilId, commercialReg)
            else -> Pair(null, null)
        }
        println("🔍 Loading first page for Change Captain (requestTypeId=$requestTypeId)")
        val firstPage = marineUnitRepository.loadShipsPage(
            ownerCivilId = civilId,
            commercialRegNumber = crNumber,
            requestTypeId = requestTypeId, // ✅ "10"
            page = 0
        )
        marineUnits = firstPage.ships
        _currentShipsPage = 0
        _isLastShipsPage = firstPage.isLastPage
        println("✅ Loaded ${marineUnits.size} ships (isLast=$_isLastShipsPage)")
        return marineUnits
    }

    override suspend fun clearLoadedShips() {
        marineUnits = emptyList()
        _currentShipsPage = -1
        _isLastShipsPage = true
    }

    override suspend fun loadNextShipsPage(formData: Map<String, String>) {
        if (_isLastShipsPage) return
        val nextPage = _currentShipsPage + 1
        val ownerCivilId = UserHelper.getOwnerCivilId(appContext)
        val commercialReg = formData["selectionData"]?.takeIf { it.isNotBlank() }
        val result = marineUnitRepository.loadShipsPage(
            ownerCivilId = ownerCivilId,
            commercialRegNumber = commercialReg,
            requestTypeId = requestTypeId,
            page = nextPage
        )
        if (result.ships.isNotEmpty()) {
            marineUnits = marineUnits + result.ships
            _currentShipsPage = nextPage
            _isLastShipsPage = result.isLastPage
            onStepsNeedRebuild?.invoke()
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Steps
    // ─────────────────────────────────────────────────────────────

    override fun getSteps(): List<StepData> {
        val steps = mutableListOf<StepData>()

        // Step 1: Person Type
        steps.add(SharedSteps.personTypeStep(typeOptions))

        // Step 2: Commercial Registration (companies only)
        if (accumulatedFormData["selectionPersonType"] == "شركة") {
            steps.add(SharedSteps.commercialRegistrationStep(commercialOptions))
        }

        // Step 3: Marine Unit Selection
        steps.add(
            SharedSteps.marineUnitSelectionStep(
                units = marineUnits,
                allowMultipleSelection = false,
                showOwnedUnitsWarning = true
            )
        )

        // Step 4: Captain/Crew Info (CDD §4.1.6)
        steps.add(
            SharedSteps.sailorInfoStep(
                includeUploadFile = false,   // No Excel upload needed for change captain
                includeDownloadFile = true,  // ✅ Show manual SailorList entry form
                jobs = crewJobTitles,
                nationalities = countryOptions,
                editNameOnly = true          // ✅ In edit mode, only allow changing name fields
            )
        )

        // Step 5: Affected Certificates (CDD §4.1.7)
        steps.add(SharedSteps.createCertificateStep(
            certificates = loadedCertificates,
            titleRes = R.string.change_captain_name_certificate_step_title,
            descriptionRes = R.string.change_captain_name_certificate_step_desc
        ))

        // Step 6: Review → send-request (CDD §4.1.9)
        steps.add(SharedSteps.reviewStep())

        // Step 7+: Payment (only after review/send-request succeeds)
        val reviewSucceeded = accumulatedFormData["sendRequestMessage"] != null
        if (reviewSucceeded) {
            steps.add(SharedSteps.paymentDetailsStep(accumulatedFormData))
            if (accumulatedFormData["paymentSuccessful"]?.toBoolean() == true) {
                steps.add(SharedSteps.paymentSuccessStep())
            }
        }

        return steps
    }

    // ─────────────────────────────────────────────────────────────
    // Context & helpers
    // ─────────────────────────────────────────────────────────────

    override fun getContext(): TransactionContext = transactionContext

    override fun updateAccumulatedData(data: Map<String, String>) {
        accumulatedFormData.putAll(data)
        println("📦 ChangeCaptain - Updated accumulated data")
    }

    override fun getFormData(): Map<String, String> = accumulatedFormData.toMap()

    override fun validateStep(step: Int, data: Map<String, Any>): Pair<Boolean, Map<String, String>> {
        val stepData = getSteps().getOrNull(step) ?: return Pair(false, emptyMap())
        return validationUseCase.validateStep(stepData, data.mapValues { it.value.toString() })
    }

    override fun setHasAcceptanceFromApi(hasAcceptanceValue: Int?) {
        super.setHasAcceptanceFromApi(hasAcceptanceValue)
        accumulatedFormData["hasAcceptance"] = (hasAcceptanceValue == 1).toString()
    }

    // ─────────────────────────────────────────────────────────────
    // processStepData
    // ─────────────────────────────────────────────────────────────

    override suspend fun processStepData(step: Int, data: Map<String, String>): Int {
        accumulatedFormData.putAll(data)
        val stepData = getSteps().getOrNull(step)
        lastApiError = null

        // ── Marine Unit Selection → proceed-request/10 (CDD §4.1.2) ───────────
        if (stepData?.titleRes == R.string.owned_ships) {
            val selectedUnitsJson = data["selectedMarineUnits"]
                ?: accumulatedFormData["selectedMarineUnits"]
            val isAddingNew = accumulatedFormData["isAddingNewUnit"]?.toBoolean() ?: false
            val hasSelectedShip = !selectedUnitsJson.isNullOrEmpty()
                && selectedUnitsJson != "[]"
                && !isAddingNew

            if (hasSelectedShip) {
                try {
                    val result = shipSelectionManager.handleShipSelection(
                        shipId = selectedUnitsJson,
                        context = transactionContext // ✅ proceed-request/10
                    )
                    when (result) {
                        is com.informatique.mtcit.business.transactions.shared.ShipSelectionResult.Success -> {
                            apiResponses["proceedRequest"] = result.response
                            val cleanJson = selectedUnitsJson!!.trim().removeSurrounding("[", "]")
                            val firstShipId = cleanJson.split(",")
                                .map { it.trim().removeSurrounding("\"") }
                                .firstOrNull()
                            firstShipId?.let {
                                val newShipId = it.toIntOrNull()
                                // ✅ If ship changed, clear cached crew so crew step reloads for the new ship
                                if (newShipId != selectedShipInfoId) {
                                    accumulatedFormData.remove("sailors")
                                    createdRequestId = null
                                    accumulatedFormData.remove("requestId")
                                    println("🔄 Ship changed from $selectedShipInfoId → $newShipId — cleared cached crew")
                                }
                                selectedShipInfoId = newShipId
                                accumulatedFormData["shipInfoId"] = it
                                accumulatedFormData["coreShipsInfoId"] = it
                            }
                            println("✅ proceed-request/10 success, shipInfoId=$firstShipId")
                        }
                        is com.informatique.mtcit.business.transactions.shared.ShipSelectionResult.Error -> {
                            lastApiError = result.message
                            throw ApiException(500, result.message)
                        }
                    }
                } catch (e: ApiException) { lastApiError = e.message ?: "error"; throw e }
                catch (e: Exception) {
                    val msg = ErrorMessageExtractor.extract(e.message)
                    lastApiError = msg; throw ApiException(500, msg)
                }
            }
        }

        // ── Crew/Captain Info → POST change-captain/{shipInfoId}/add-request (CDD §4.1.6) ───
        if (stepData?.stepType == StepType.CREW_MANAGEMENT) {
            val shipInfoId = selectedShipInfoId
                ?: accumulatedFormData["shipInfoId"]?.toIntOrNull()
                ?: throw ApiException(400, "معرف السفينة غير موجود")

            try {
                // Parse crew from form, converting Map<String,Any> → Map<String,String>
                // ✅ Strip "id"/"apiId" keys so the API receives no id field (CDD §4.1.6)
                val rawCrew: List<Map<String, Any>> = navigationLicenseManager.parseCrewFromFormData(data)
                val crewList: List<Map<String, String>> = rawCrew.map { crew ->
                    crew
                        .filterKeys { k -> k != "id" && k != "apiId" }
                        .mapValues { (_, v) ->
                            when (v) {
                                is Map<*, *> -> v["id"]?.toString() ?: v.values.firstOrNull()?.toString() ?: ""
                                null -> ""
                                else -> v.toString()
                            }
                        }
                }
                if (crewList.isEmpty()) throw ApiException(400, "يرجى إضافة بيانات الربان")

                println("📤 POST change-captain/$shipInfoId/add-request (${crewList.size} crew)")

                val result = navigationLicenseManager.createChangeCaptainRequest(
                    shipInfoId = shipInfoId.toLong(),
                    crewList = crewList
                )
                result.onSuccess { dto ->
                    createdRequestId = dto.id
                    accumulatedFormData["requestId"] = dto.id.toString()
                    dto.requestSerial?.let { accumulatedFormData["requestSerial"] = it.toString() }
                    println("✅ Change captain request created, id=${dto.id}")
                }
                result.onFailure { err ->
                    val msg = err.message ?: "فشل إنشاء طلب تغيير الربان"
                    lastApiError = msg; throw ApiException(500, msg)
                }
            } catch (e: ApiException) { lastApiError = e.message ?: "error"; throw e }
            catch (e: Exception) {
                val msg = ErrorMessageExtractor.extract(e.message)
                lastApiError = msg; throw ApiException(500, msg)
            }
        }

        // ── Review → POST general-request/10/{requestId}/send-request (CDD §4.1.9) ──────────
        if (stepData?.stepType == StepType.REVIEW) {
            val requestIdInt = createdRequestId?.toInt()
                ?: accumulatedFormData["requestId"]?.toIntOrNull()
                ?: throw ApiException(400, "معرف الطلب غير موجود")

            try {
                val reviewResult = reviewManager.processReviewStep(
                    endpoint = transactionContext.sendRequestEndpoint,
                    requestId = requestIdInt,
                    transactionName = transactionContext.displayName,
                    sendRequestPostOrPut = transactionContext.sendRequestPostOrPut
                )

                when (reviewResult) {
                    is com.informatique.mtcit.business.transactions.shared.ReviewResult.Success -> {
                        accumulatedFormData["sendRequestMessage"] = reviewResult.message
                        val requestNumber = reviewResult.additionalData?.get("requestSerial")?.toString()
                            ?: accumulatedFormData["requestSerial"]
                            ?: requestIdInt.toString()
                        accumulatedFormData["requestNumber"] = requestNumber

                        val hasAcceptance = accumulatedFormData["hasAcceptance"]?.toBoolean() ?: false
                        if (hasAcceptance) {
                            accumulatedFormData["successMessage"] = reviewResult.message
                            accumulatedFormData["requestSubmitted"] = "true"
                            return -2
                        }
                        onStepsNeedRebuild?.invoke()
                    }
                    is com.informatique.mtcit.business.transactions.shared.ReviewResult.Error -> {
                        lastApiError = reviewResult.message
                        throw ApiException(500, reviewResult.message)
                    }
                }
            } catch (e: ApiException) {
                // ✅ Re-throw ApiException (e.g. 401) so ViewModel can show refresh button
                println("❌ ApiException in review step: ${e.code} - ${e.message}")
                throw e
            } catch (e: Exception) {
                println("❌ Exception in review step: ${e.message}")
                e.printStackTrace()
                lastApiError = e.message ?: "حدث خطأ أثناء إرسال الطلب"
                throw ApiException(500, e.message ?: "حدث خطأ أثناء إرسال الطلب")
            }
        }

        // ── Payment (CDD §4.1.11-12) ──────────────────────────────────────────
        if (stepData?.stepType == StepType.PAYMENT) {
            val paymentResult = paymentManager.processStepIfNeeded(
                stepType = StepType.PAYMENT,
                formData = accumulatedFormData,
                requestTypeId = requestTypeId.toInt(),
                context = transactionContext
            )
            when (paymentResult) {
                is StepProcessResult.Success -> {
                    onStepsNeedRebuild?.invoke()
                    if (accumulatedFormData["showPaymentSuccessDialog"]?.toBoolean() == true) {
                        return step
                    }
                }
                is StepProcessResult.Error -> {
                    lastApiError = paymentResult.message
                    return -1
                }
                is StepProcessResult.NoAction -> {}
            }
        }

        return step
    }

    // ─────────────────────────────────────────────────────────────
    // Stubs required by BaseTransactionStrategy
    // ─────────────────────────────────────────────────────────────

    override suspend fun submit(data: Map<String, String>): Result<Boolean> = Result.success(true)

    override fun extractCompletedStepsFromApiResponse(response: Any): Set<StepType> = emptySet()
}
