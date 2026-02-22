package com.informatique.mtcit.data.repository

import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.PortOfRegistry
import com.informatique.mtcit.data.model.UserRequest
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
     * Get engineer inspection requests with pagination
     * Uses the engineer-specific filtered API endpoint with simplified filter structure
     *
     * @param page Page number (0-based)
     * @param size Number of items per page
     * @param searchText Optional search text
     * @param columnName Column to search in
     * @return Result with RequestsApiResponse
     */
    suspend fun getEngineerInspectionRequests(
        page: Int = 0,
        size: Int = 10,
        searchText: String = "",
        columnName: String = "requestNumber"
    ): Result<com.informatique.mtcit.data.model.requests.RequestsApiResponse> {
        return userRequestsRepository.getEngineerInspectionRequests(page, size, searchText, columnName)
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
                TransactionType.SHIP_PORT_CHANGE -> "/api/v1/change-ship-info"
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
        val formData = buildFormDataFromJson(jsonObject).toMutableMap()

        // ✅ CRITICAL: Ensure deletionRequestId is also stored for cancel registration
        formData["deletionRequestId"] = id.toString()
        println("💾 Stored requestId=$id and deletionRequestId=$id in formData for payment step")

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

        // Get current language for localized names
        val currentLanguage = java.util.Locale.getDefault().language

        // ✅ Extract engine data as JSON array for EngineListManager
        val enginesArray = shipInfoObject?.get("shipInfoEngines")?.jsonArray
        if (!enginesArray.isNullOrEmpty()) {
            val enginesList = mutableListOf<Map<String, String>>()

            enginesArray.forEach { engineItem ->
                val engineInfo = engineItem.jsonObject
                val engineObject = engineInfo["engine"]?.jsonObject
                val engineDocsArray = engineInfo["engineDocs"]?.jsonArray

                engineObject?.let { engine ->
                    val engineMap = mutableMapOf<String, String>()

                    // Generate unique ID for this engine
                    engineMap["id"] = java.util.UUID.randomUUID().toString()

                    // ✅ CRITICAL: Store database ID to track existing engines (OUTER id from shipInfoEngines)
                    engineInfo["id"]?.jsonPrimitive?.intOrNull?.let {
                        engineMap["dbId"] = it.toString()
                        println("   ✅ Engine dbId (shipInfoEngines): $it")
                    }

                    // Extract engine fields
                    engine["engineSerialNumber"]?.jsonPrimitive?.contentOrNull?.let { engineMap["number"] = it }
                    engine["engineModel"]?.jsonPrimitive?.contentOrNull?.let { engineMap["model"] = it }
                    engine["engineManufacturer"]?.jsonPrimitive?.contentOrNull?.let { engineMap["manufacturer"] = it }
                    engine["enginePower"]?.jsonPrimitive?.intOrNull?.let { engineMap["power"] = it.toString() }
                    engine["cylindersCount"]?.jsonPrimitive?.intOrNull?.let { engineMap["cylinder"] = it.toString() }
                    engine["engineBuildYear"]?.jsonPrimitive?.contentOrNull?.let { engineMap["manufactureYear"] = it }

                    // Extract engine type (localized)
                    val engineTypeObject = engine["engineType"]?.jsonObject
                    engineTypeObject?.let { type ->
                        val nameAr = type["nameAr"]?.jsonPrimitive?.contentOrNull
                        val nameEn = type["nameEn"]?.jsonPrimitive?.contentOrNull
                        val localizedType = if (currentLanguage == "ar") nameAr else nameEn
                        localizedType?.let { engineMap["type"] = it }
                    }

                    // Extract engine country (localized)
                    val engineCountryObject = engine["engineCountry"]?.jsonObject
                    engineCountryObject?.let { country ->
                        val nameAr = country["nameAr"]?.jsonPrimitive?.contentOrNull
                        val nameEn = country["nameEn"]?.jsonPrimitive?.contentOrNull
                        val localizedCountry = if (currentLanguage == "ar") nameAr else nameEn
                        localizedCountry?.let { engineMap["productionCountry"] = it }
                    }

                    // Extract engine fuel type (localized)
                    val engineFuelTypeObject = engine["engineFuelType"]?.jsonObject
                    engineFuelTypeObject?.let { fuel ->
                        val nameAr = fuel["nameAr"]?.jsonPrimitive?.contentOrNull
                        val nameEn = fuel["nameEn"]?.jsonPrimitive?.contentOrNull
                        val localizedFuel = if (currentLanguage == "ar") nameAr else nameEn
                        localizedFuel?.let { engineMap["fuelType"] = it }
                    }

                    // Extract engine status (localized)
                    val engineStatusObject = engine["engineStatus"]?.jsonObject
                    engineStatusObject?.let { status ->
                        val nameAr = status["nameAr"]?.jsonPrimitive?.contentOrNull
                        val nameEn = status["nameEn"]?.jsonPrimitive?.contentOrNull
                        val localizedStatus = if (currentLanguage == "ar") nameAr else nameEn
                        localizedStatus?.let { engineMap["condition"] = it }
                    }

                    // ✅ Extract engine document if exists (from draft)
                    engineDocsArray?.firstOrNull()?.jsonObject?.let { doc ->
                        // Store docRefNum for preview and PUT requests
                        doc["docRefNum"]?.jsonPrimitive?.contentOrNull?.let { refNum ->
                            engineMap["documentRefNum"] = refNum
                            engineMap["documentUri"] = "draft:$refNum" // Mark as draft document
                            println("   📎 Engine has document: refNum=$refNum")
                        }

                        // Store file name for display
                        doc["fileName"]?.jsonPrimitive?.contentOrNull?.let { fileName ->
                            engineMap["documentFileName"] = fileName
                        }
                    }

                    enginesList.add(engineMap)
                }
            }

            // Convert to JSON array string
            if (enginesList.isNotEmpty()) {
                formData["engines"] = Json.encodeToString(enginesList)
                println("✅ Parsed ${enginesList.size} engines from draft")
            }
        }

        // ✅ Extract owner data as JSON array for OwnerListManager
        val ownersArray = shipInfoObject?.get("shipInfoOwners")?.jsonArray
        if (!ownersArray.isNullOrEmpty()) {
            val ownersList = mutableListOf<Map<String, String>>()

            ownersArray.forEach { ownerItem ->
                val ownerInfo = ownerItem.jsonObject
                val ownerObject = ownerInfo["owner"]?.jsonObject
                // ✅ FIX: API returns "documents", not "ownerDocs"
                val ownerDocsArray = ownerInfo["documents"]?.jsonArray

                ownerObject?.let { owner ->
                    val ownerMap = mutableMapOf<String, String>()

                    // Generate unique ID for this owner
                    ownerMap["id"] = java.util.UUID.randomUUID().toString()

                    // ✅ CRITICAL: Store database ID to track existing owners
                    // Use the OUTER id (shipInfoOwners[].id), not the inner id (shipInfoOwners[].owner.id)
                    ownerInfo["id"]?.jsonPrimitive?.intOrNull?.let {
                        ownerMap["dbId"] = it.toString()
                        println("   ✅ Owner dbId: $it")
                    }

                    // Extract owner fields
                    owner["ownerName"]?.jsonPrimitive?.contentOrNull?.let { ownerMap["ownerName"] = it }
                    owner["ownerNameEn"]?.jsonPrimitive?.contentOrNull?.let { ownerMap["ownerNameEn"] = it }
                    owner["ownerCivilId"]?.jsonPrimitive?.contentOrNull?.let { ownerMap["idNumber"] = it }
                    owner["ownerPhone"]?.jsonPrimitive?.contentOrNull?.let { ownerMap["mobile"] = it }
                    owner["ownerEmail"]?.jsonPrimitive?.contentOrNull?.let { ownerMap["email"] = it }
                    owner["ownerAddress"]?.jsonPrimitive?.contentOrNull?.let { ownerMap["address"] = it }

                    // Extract ownership percentage
                    ownerInfo["ownershipPercentage"]?.jsonPrimitive?.intOrNull?.let {
                        ownerMap["ownerShipPercentage"] = it.toString()
                    }

                    // Check if representative
                    owner["isRepresentative"]?.jsonPrimitive?.intOrNull?.let { isRep ->
                        ownerMap["isRepresentative"] = (isRep == 1).toString()
                    }

                    // ✅ Extract owner document if exists (from draft)
                    ownerDocsArray?.firstOrNull()?.jsonObject?.let { doc ->
                        // Store docRefNum for preview and PUT requests
                        doc["docRefNum"]?.jsonPrimitive?.contentOrNull?.let { refNum ->
                            ownerMap["ownershipProofDocumentRefNum"] = refNum
                            ownerMap["ownershipProofDocument"] = "draft:$refNum" // Mark as draft document
                            println("   📎 Owner has document: refNum=$refNum")
                        }

                        // Store file name for display
                        doc["fileName"]?.jsonPrimitive?.contentOrNull?.let { fileName ->
                            ownerMap["ownershipProofDocumentFileName"] = fileName
                        }
                    }

                    ownersList.add(ownerMap)
                }
            }

            // Convert to JSON array string
            if (ownersList.isNotEmpty()) {
                formData["owners"] = Json.encodeToString(ownersList)
                // Also set total owners count
                formData["totalOwnersCount"] = ownersList.size.toString()
                println("✅ Parsed ${ownersList.size} owners from draft")
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
