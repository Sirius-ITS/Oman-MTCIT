package com.informatique.mtcit.data.repository

import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.PortOfRegistry
import com.informatique.mtcit.data.model.UserRequest
import kotlinx.coroutines.delay
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing user requests (الاستمارات)
 *
 * Handles request status checking and progress saving
 * Uses UserRequestsRepository for real API calls
 */
@Singleton
class RequestRepository @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRequestsRepository: UserRequestsRepository
) {

    /**
     * Get filtered user requests with pagination
     * Uses the new filtered API endpoint
     *
     * @param civilId User's civil ID
     * @param filter RequestFilterDto containing filter criteria and pagination
     * @return Result with RequestsApiResponse
     */
    suspend fun getFilteredUserRequests(
        civilId: String,
        filter: com.informatique.mtcit.data.model.requests.RequestFilterDto
    ): Result<com.informatique.mtcit.data.model.requests.RequestsApiResponse> {
        return userRequestsRepository.getFilteredUserRequests(civilId, filter)
    }

    /**
     * Get request status by ID - Calls real API
     *
     * API: GET /api/v1/{endpoint}/{requestId}
     * @param requestId Request ID (e.g., "1844")
     * @param transactionType Transaction type to determine correct API endpoint
     * @return Result with UserRequest containing latest status and data
     */
    suspend fun getRequestStatus(requestId: String, transactionType: TransactionType): Result<UserRequest> {
        return try {
            println("🌐 RequestRepository: Fetching request $requestId from API")

            // ✅ Determine endpoint path based on transaction type
            val endpointPath = when (transactionType) {
                TransactionType.REQUEST_FOR_INSPECTION -> "/api/v1/inspection-requests"
                TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE -> "/api/v1/registration-requests"
                TransactionType.PERMANENT_REGISTRATION_CERTIFICATE -> "/api/v1/perm-registration-requests"
                TransactionType.ISSUE_NAVIGATION_PERMIT -> "/api/v1/ship-navigation-license-request"
                TransactionType.RENEW_NAVIGATION_PERMIT -> "/api/v1/navigation-license-renewal-request"
                TransactionType.MORTGAGE_CERTIFICATE -> "/api/v1/mortgage-request"
                TransactionType.RELEASE_MORTGAGE -> "/api/v1/mortgage-redemption-request"
                TransactionType.CANCEL_PERMANENT_REGISTRATION -> "/api/v1/deletion-requests"
                else -> ""
            }

            println("📡 Using endpoint: $endpointPath for transactionType: $transactionType")

            // Call real API
            val result = userRequestsRepository.getRequestDetail(
                requestId = requestId.toInt(),
                endpointPath = endpointPath
            )

            result.fold(
                onSuccess = { response ->
                    println("✅ RequestRepository: API returned request $requestId")

                    // Parse the JSON response to extract data
                    val jsonData = response.data
                    // ✅ Pass transactionType to parser so it doesn't try to extract it from JSON
                    val mappedRequest = parseApiResponseToUserRequest(jsonData, requestId, transactionType)

                    println("✅ RequestRepository: Mapped to UserRequest with statusId ${mappedRequest.statusId}")
                    Result.success(mappedRequest)
                },
                onFailure = { error ->
                    println("❌ RequestRepository: API call failed: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            println("❌ RequestRepository: Error getting request status: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Save request progress (when inspection is pending)
     * Mock implementation - will be replaced with real API call
     */
    suspend fun saveRequestProgress(
        userId: String,
        transactionType: TransactionType,
        marineUnit: MarineUnit?,
        formData: Map<String, String>,
        lastCompletedStep: Int,
        statusId: Int = 5 // Default: Pending
    ): Result<String> {
        return try {
            delay(400)

            val requestId = "REQ_${System.currentTimeMillis()}"

            println("✅ RequestRepository: Saved request $requestId for user $userId with statusId $statusId")

            Result.success(requestId)
        } catch (e: Exception) {
            println("❌ RequestRepository: Error saving request: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Parse API JSON response to UserRequest model
     * @param jsonData JSON data from API response
     * @param requestId Request ID
     * @param knownTransactionType Transaction type (known from navigation context)
     */
    private fun parseApiResponseToUserRequest(
        jsonData: kotlinx.serialization.json.JsonElement,
        requestId: String,
        knownTransactionType: TransactionType
    ): UserRequest {
        val jsonObject = jsonData.jsonObject

        // Extract basic fields
        val id = jsonObject["id"]?.jsonPrimitive?.intOrNull ?: 0
        val requestSerial = jsonObject["requestSerial"]?.jsonPrimitive?.intOrNull ?: 0

        // Extract status (handle both "status" and "requestStatus" keys for inspection requests)
        val statusObject = (jsonObject["status"] ?: jsonObject["requestStatus"])?.jsonObject
        val statusId = statusObject?.get("id")?.jsonPrimitive?.intOrNull ?: 5

        // ✅ Use the known transaction type from parameter (don't try to parse from JSON)
        // Inspection requests don't have "requestType" field, so we use the known type
        val transactionType = knownTransactionType

        // Extract ship info
        val shipInfoObject = jsonObject["shipInfo"]?.jsonObject
        val shipObject = shipInfoObject?.get("ship")?.jsonObject

        val marineUnit = shipObject?.let { ship ->
            val shipId = ship["id"]?.jsonPrimitive?.intOrNull ?: 0
            val callSign = ship["callSign"]?.jsonPrimitive?.contentOrNull ?: ""
            val imoNumber = ship["imoNumber"]?.jsonPrimitive?.intOrNull?.toString() ?: ""
            val mmsiNumber = ship["mmsiNumber"]?.jsonPrimitive?.longOrNull?.toString() ?: ""

            val portObject = ship["portOfRegistry"]?.jsonObject
            val portName = portObject?.get("nameEn")?.jsonPrimitive?.contentOrNull
                ?: portObject?.get("nameAr")?.jsonPrimitive?.contentOrNull
                ?: ""

            val length = ship["vesselLengthOverall"]?.jsonPrimitive?.intOrNull?.toString() ?: ""
            val width = ship["vesselBeam"]?.jsonPrimitive?.intOrNull?.toString() ?: ""
            val height = ship["vesselHeight"]?.jsonPrimitive?.intOrNull?.toString() ?: ""

            MarineUnit(
                id = shipId.toString(),
                shipName = callSign,
                callSign = callSign,
                imoNumber = imoNumber,
                mmsiNumber = mmsiNumber,
                portOfRegistry = PortOfRegistry(portName),
                totalLength = length,
                totalWidth = width,
                height = height
            )
        }

        // Build form data from API response
        val formData = buildFormDataFromJson(jsonObject)

        // Extract messages
        val message = jsonObject["message"]?.jsonPrimitive?.contentOrNull
        val messageDetails = jsonObject["messageDetails"]?.jsonPrimitive?.contentOrNull

        // ✅ SMART: Determine last completed step based on status AND transaction type
        // For ACCEPTED status (7), the review step is COMPLETED, so we resume AFTER review
        // For Temporary Registration: Resume at Marine Unit Name Selection (step after Review)
        // For other transactions: Resume at Payment (step after Review)
        val lastCompletedStep = when {
            // ✅ ACCEPTED status - review is COMPLETED, resume at next step
            statusId == 7 -> {
                // ✅ SMART: Return index of REVIEW step (typically 8)
                // The findResumeStepByType function will find the correct NEXT step
                // based on each strategy's actual step structure:
                // - Temp Registration: Has CUSTOM (Marine Unit Name) after REVIEW
                // - Other transactions: Have PAYMENT directly after REVIEW
                println("✅ ACCEPTED status - Review completed, will resume at next step after REVIEW")
                8  // Index of REVIEW step - findResumeStepByType will find the correct next step
            }
            // Confirmed, Approved by Authorities, Final Approval
            statusId in listOf(3, 11, 12) -> 8
            // Action Taken, Issued - everything done
            statusId in listOf(13, 14) -> 10
            // Default
            else -> 0
        }

        println("🔍 RequestRepository: statusId=$statusId, transactionType=$transactionType, lastCompletedStep=$lastCompletedStep")

        return UserRequest(
            id = requestId,
            userId = "currentUserId",
            type = transactionType,
            statusId = statusId,
            marineUnit = marineUnit,
            createdDate = requestSerial.toString(),
            lastUpdatedDate = requestSerial.toString(),
            formData = formData,
            lastCompletedStep = lastCompletedStep,
            rejectionReason = if (statusId in listOf(2, 10)) messageDetails else null,
            estimatedCompletionDate = null
        )
    }

    /**
     * Build form data map from JSON response
     */
    private fun buildFormDataFromJson(jsonObject: kotlinx.serialization.json.JsonObject): Map<String, String> {
        val formData = mutableMapOf<String, String>()

        // Add request metadata
        val id = jsonObject["id"]?.jsonPrimitive?.intOrNull
        val requestSerial = jsonObject["requestSerial"]?.jsonPrimitive?.intOrNull

        if (id != null) formData["requestId"] = id.toString()
        if (requestSerial != null) formData["requestSerial"] = requestSerial.toString()

        // Extract ship info
        val shipInfoObject = jsonObject["shipInfo"]?.jsonObject
        val shipObject = shipInfoObject?.get("ship")?.jsonObject

        shipInfoObject?.let {
            val shipInfoId = it["id"]?.jsonPrimitive?.intOrNull
            if (shipInfoId != null) {
                formData["shipInfoId"] = shipInfoId.toString()
                formData["coreShipsInfoId"] = shipInfoId.toString()
            }
        }

        // ✅ CRITICAL FIX: If ship data exists, mark that unit was already added
        // This tells the strategy to generate all the detail steps
        if (shipObject != null) {
            formData["isAddingNewUnit"] = "true"
            println("✅ Setting isAddingNewUnit=true because ship data exists in API response")
        }

        shipObject?.let { ship ->
            println("🔍 DEBUG - Parsing ship data from draft...")
            println("   Current language: ${java.util.Locale.getDefault().language}")

            // ✅ Basic ship identification fields
            ship["callSign"]?.jsonPrimitive?.contentOrNull?.let {
                formData["callSign"] = it
                println("   ✅ callSign: $it")
            }
            ship["imoNumber"]?.jsonPrimitive?.intOrNull?.let {
                formData["imoNumber"] = it.toString()
                println("   ✅ imoNumber: $it")
            }

            // ✅ FIX: MMSI uses "mmsi" in the form, not "mmsiNumber"
            ship["mmsiNumber"]?.jsonPrimitive?.longOrNull?.let {
                formData["mmsi"] = it.toString()
                println("   ✅ mmsi (from mmsiNumber): $it")
            }

            ship["officialNumber"]?.jsonPrimitive?.contentOrNull?.let {
                formData["officialNumber"] = it
                println("   ✅ officialNumber: $it")
            }

            // ✅ Dimensions - using correct field names
            ship["vesselLengthOverall"]?.jsonPrimitive?.intOrNull?.let {
                formData["overallLength"] = it.toString()
                println("   ✅ overallLength: $it")
            }
            ship["vesselBeam"]?.jsonPrimitive?.intOrNull?.let {
                formData["overallWidth"] = it.toString()
                println("   ✅ overallWidth: $it")
            }
            ship["vesselDraft"]?.jsonPrimitive?.intOrNull?.let {
                formData["depth"] = it.toString()
                println("   ✅ depth: $it")
            }
            ship["vesselHeight"]?.jsonPrimitive?.intOrNull?.let {
                formData["height"] = it.toString()
                println("   ✅ height: $it")
            }
            ship["decksNumber"]?.jsonPrimitive?.intOrNull?.let {
                formData["decksCount"] = it.toString()
                println("   ✅ decksCount: $it")
            }

            // ✅ Weights
            ship["grossTonnage"]?.jsonPrimitive?.intOrNull?.let {
                formData["grossTonnage"] = it.toString()
                println("   ✅ grossTonnage: $it")
            }
            ship["netTonnage"]?.jsonPrimitive?.intOrNull?.let {
                formData["netTonnage"] = it.toString()
                println("   ✅ netTonnage: $it")
            }
            ship["deadweightTonnage"]?.jsonPrimitive?.intOrNull?.let {
                formData["staticLoad"] = it.toString()
                println("   ✅ staticLoad (from deadweightTonnage): $it")
            }
            ship["maxLoadCapacity"]?.jsonPrimitive?.intOrNull?.let {
                formData["maxPermittedLoad"] = it.toString()
                println("   ✅ maxPermittedLoad: $it")
            }

            // ✅ Dates
            ship["shipBuildYear"]?.jsonPrimitive?.intOrNull?.let {
                formData["manufacturerYear"] = it.toString()
                println("   ✅ manufacturerYear: $it")
            }
            ship["buildEndDate"]?.jsonPrimitive?.contentOrNull?.let {
                formData["constructionEndDate"] = it
                println("   ✅ constructionEndDate: $it")
            }
            ship["firstRegistrationDate"]?.jsonPrimitive?.contentOrNull?.let {
                formData["firstRegistrationDate"] = it
                println("   ✅ firstRegistrationDate: $it")
            }

            // ✅ FIX: Use LOCALIZED names (getLocalizedName) so dropdowns can find them
            // This matches how LookupRepository returns data
            val currentLanguage = java.util.Locale.getDefault().language

            val portObject = ship["portOfRegistry"]?.jsonObject
            portObject?.let { port ->
                val nameAr = port["nameAr"]?.jsonPrimitive?.contentOrNull
                val nameEn = port["nameEn"]?.jsonPrimitive?.contentOrNull
                val localizedName = if (currentLanguage == "ar") nameAr else nameEn
                localizedName?.let {
                    formData["registrationPort"] = it
                    println("   ✅ registrationPort: $it (ar=$nameAr, en=$nameEn)")
                }
            }

            val shipTypeObject = ship["shipType"]?.jsonObject
            shipTypeObject?.let { type ->
                val nameAr = type["nameAr"]?.jsonPrimitive?.contentOrNull
                val nameEn = type["nameEn"]?.jsonPrimitive?.contentOrNull
                val localizedName = if (currentLanguage == "ar") nameAr else nameEn
                localizedName?.let {
                    formData["unitType"] = it
                    println("   ✅ unitType: $it (ar=$nameAr, en=$nameEn)")
                }
            }

            val shipCategoryObject = ship["shipCategory"]?.jsonObject
            shipCategoryObject?.let { category ->
                val nameAr = category["nameAr"]?.jsonPrimitive?.contentOrNull
                val nameEn = category["nameEn"]?.jsonPrimitive?.contentOrNull
                val localizedName = if (currentLanguage == "ar") nameAr else nameEn
                localizedName?.let {
                    formData["unitClassification"] = it
                    println("   ✅ unitClassification: $it (ar=$nameAr, en=$nameEn)")
                }
            }

            val activityObject = ship["marineActivity"]?.jsonObject
            activityObject?.let { activity ->
                val nameAr = activity["nameAr"]?.jsonPrimitive?.contentOrNull
                val nameEn = activity["nameEn"]?.jsonPrimitive?.contentOrNull
                val localizedName = if (currentLanguage == "ar") nameAr else nameEn
                localizedName?.let {
                    formData["maritimeActivity"] = it
                    println("   ✅ maritimeActivity: $it (ar=$nameAr, en=$nameEn)")
                }
            }

            val buildCountryObject = ship["buildCountry"]?.jsonObject
            buildCountryObject?.let { country ->
                val nameAr = country["nameAr"]?.jsonPrimitive?.contentOrNull
                val nameEn = country["nameEn"]?.jsonPrimitive?.contentOrNull
                val localizedName = if (currentLanguage == "ar") nameAr else nameEn
                localizedName?.let {
                    formData["registrationCountry"] = it
                    println("   ✅ registrationCountry: $it (ar=$nameAr, en=$nameEn)")
                }
            }

            val proofTypeObject = ship["proofType"]?.jsonObject
            proofTypeObject?.let { proof ->
                val nameAr = proof["nameAr"]?.jsonPrimitive?.contentOrNull
                val nameEn = proof["nameEn"]?.jsonPrimitive?.contentOrNull
                val localizedName = if (currentLanguage == "ar") nameAr else nameEn
                localizedName?.let {
                    formData["proofType"] = it
                    println("   ✅ proofType: $it (ar=$nameAr, en=$nameEn)")
                }
            }

            val buildMaterialObject = ship["buildMaterial"]?.jsonObject
            buildMaterialObject?.let { material ->
                val nameAr = material["nameAr"]?.jsonPrimitive?.contentOrNull
                val nameEn = material["nameEn"]?.jsonPrimitive?.contentOrNull
                val localizedName = if (currentLanguage == "ar") nameAr else nameEn
                localizedName?.let {
                    formData["buildingMaterial"] = it
                    println("   ✅ buildingMaterial: $it (ar=$nameAr, en=$nameEn)")
                }
            }

            println("✅ Draft parsing complete - ${formData.size} fields mapped")
        }

        // Extract engine data
        val enginesArray = shipInfoObject?.get("shipInfoEngines")?.jsonArray
        enginesArray?.firstOrNull()?.jsonObject?.let { engineInfo ->
            val engineObject = engineInfo["engine"]?.jsonObject
            engineObject?.let { engine ->
                engine["engineManufacturer"]?.jsonPrimitive?.contentOrNull?.let { formData["engineManufacturer"] = it }
                engine["engineModel"]?.jsonPrimitive?.contentOrNull?.let { formData["engineModel"] = it }
                engine["enginePower"]?.jsonPrimitive?.intOrNull?.let { formData["enginePower"] = it.toString() }

                val engineTypeObject = engine["engineType"]?.jsonObject
                engineTypeObject?.get("nameEn")?.jsonPrimitive?.contentOrNull?.let { formData["fuelType"] = it }

                val engineCountryObject = engine["engineCountry"]?.jsonObject
                engineCountryObject?.get("nameEn")?.jsonPrimitive?.contentOrNull?.let { formData["engineManufacturerCountry"] = it }
            }
        }

        // Extract owner data
        val ownersArray = shipInfoObject?.get("shipInfoOwners")?.jsonArray
        ownersArray?.firstOrNull()?.jsonObject?.let { ownerInfo ->
            val ownerObject = ownerInfo["owner"]?.jsonObject
            ownerObject?.let { owner ->
                owner["ownerName"]?.jsonPrimitive?.contentOrNull?.let { formData["ownerName"] = it }
                owner["ownerCivilId"]?.jsonPrimitive?.contentOrNull?.let { formData["ownerCivilId"] = it }
                owner["ownerPhone"]?.jsonPrimitive?.contentOrNull?.let { formData["ownerPhone"] = it }
                owner["ownerEmail"]?.jsonPrimitive?.contentOrNull?.let { formData["ownerEmail"] = it }
                owner["ownerAddress"]?.jsonPrimitive?.contentOrNull?.let { formData["ownerAddress"] = it }
            }
        }

        // ✅ Extract documents data - Check if documents exist and are not empty
        val documentsArray = jsonObject["documents"]?.jsonArray
        if (!documentsArray.isNullOrEmpty()) {
            formData["hasDocuments"] = "true"
            println("✅ Draft has ${documentsArray.size} documents uploaded")
        }

        return formData
    }
}
