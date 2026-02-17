package com.informatique.mtcit.data.api

import android.util.Base64
import com.informatique.mtcit.business.transactions.shared.BuildCountry
import com.informatique.mtcit.business.transactions.shared.BuildMaterial
import com.informatique.mtcit.business.transactions.shared.MarineActivity
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.PortOfRegistry
import com.informatique.mtcit.business.transactions.shared.ProofType
import com.informatique.mtcit.business.transactions.shared.ShipCategory
import com.informatique.mtcit.business.transactions.shared.ShipType
import com.informatique.mtcit.common.ErrorMessageExtractor
import com.informatique.mtcit.data.model.ProceedRequestResponse
import com.informatique.mtcit.di.module.AppRepository
import com.informatique.mtcit.di.module.RepoServiceState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarineUnitsApiService @Inject constructor(
    val repo: AppRepository,
    private val json: Json
) {

    /**
     * Get all ships for the current user
     *
     * API Documentation:
     * - Filter must be Base64 encoded
     * - For individuals (after PKI auth): send ownerId
     * - For companies (after PKI auth): send ownerId + commercialNumber (CR Number)
     * - requestTypeId: Transaction ID from navigation (e.g., 7 for temp cert, 8 for permanent, etc.)
     *
     * API Required JSON Shape:
     * {
     *   "requestTypeId": 0,           // Must be a number
     *   "commercialNumber": "string", // CR Number for companies (not company name)
     *   "ownerId": "string"           // Owner civil ID
     * }
     *
     * Behavior changes:
     * - The API call will NOT be performed unless `stepActive` is true. This prevents
     *   automatic network calls when the transaction flow hasn't reached the ships step.
     * - Pass `ownerCivilId` for individuals or `commercialRegNumber` (CR Number) for companies.
     * - Pass `requestTypeId` to filter ships based on transaction type.
     * - For local testing you can set `useTestCivilId = true` to use a fixed civil ID.
     *   When `useTestCivilId` is true, the call will be executed even if `stepActive` is false
     *   so you can test without changing callers everywhere.
     */
    suspend fun getMyShips(
        ownerCivilId: String? = null,
        commercialRegNumber: String? = null, // ✅ This should be the CR Number, NOT the company name
        requestTypeId: String? = null,
        stepActive: Boolean = false,
        useTestCivilId: Boolean = false
    ): Result<List<MarineUnit>> {
        return try {
            // If the step is not active, don't call the API unless test-mode override is enabled.
            if (!stepActive && !useTestCivilId) {
                println("⏸ getMyShips: call suppressed because stepActive=false. Provide stepActive=true when you want to fetch ships.")
                return Result.failure(IllegalStateException("getMyShips suppressed: step not active"))
            }

            if (useTestCivilId) {
                println("🧪 getMyShips: stepActive is false but useTestCivilId=true => forcing call with test civil id")
            }

            println("🔍 Input parameters:")
            println("   - ownerCivilId: $ownerCivilId")
            println("   - commercialRegNumber (CR Number): $commercialRegNumber")
            println("   - requestTypeId: $requestTypeId")
            println("   - stepActive: $stepActive")
            println("   - useTestCivilId: $useTestCivilId")

            // Build filter JSON in the shape required by the API:
            // For companies: { "requestTypeId": 0, "commercialNumber": "string", "ownerId": "string" }
            // For individuals: { "requestTypeId": 0, "ownerId": "string" } ← commercialNumber NOT included
            // - requestTypeId must be a number (default 0)
            // - commercialNumber should contain the CR Number (only for companies)
            // - ownerId is the owner civil id (required)
            val requestTypeInt = requestTypeId?.toIntOrNull() ?: 0
            val commercialNumberForFilter = commercialRegNumber?.takeIf { it.isNotBlank() }
            val ownerIdForFilter = when {
                !ownerCivilId.isNullOrBlank() -> ownerCivilId
                useTestCivilId -> "12345678"
                else -> ""
            }

            if (ownerIdForFilter.isBlank()) {
                println("❌ No ownerId provided (required)")
                return Result.failure(IllegalArgumentException("ownerId is required"))
            }

            // Use kotlinx.serialization to build a safe JSON object (handles non-ascii content)
            // ✅ Only include commercialNumber if it has a value (for companies)
            val filterJsonElement = kotlinx.serialization.json.buildJsonObject {
                put("requestTypeId", kotlinx.serialization.json.JsonPrimitive(requestTypeInt))
                // ✅ Only add commercialNumber field if it exists (companies only)
                if (commercialNumberForFilter != null) {
                    put("commercialNumber", kotlinx.serialization.json.JsonPrimitive(commercialNumberForFilter))
                }
                put("ownerId", kotlinx.serialization.json.JsonPrimitive(ownerIdForFilter))
            }

            val filterJson = filterJsonElement.toString()

            // Base64 encode the filter
            val base64Filter = Base64.encodeToString(
                filterJson.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )

            val filterParam = "?filter=$base64Filter"

            println("🔍 Fetching ships with filter: $filterJson")
            println("📋 Base64 encoded filter: $base64Filter")

            // ✅ FIXED: Make endpoint dynamic based on requestTypeId
            // Different endpoints for different transaction types:
            // 1 = Temporary Registration → registration-requests/get-my-ships
            // 2 = Permanent Registration → perm-registration-requests/get-my-ships
            // 3 = Deletion → deletion-requests/get-my-ships
            // 4 = Mortgage → mortgage-request/get-my-ships
            // 5 = Release Mortgage → mortgage-redemption-request/get-my-ships
            val baseEndpoint = when (requestTypeInt) {
                1 -> "registration-requests/get-my-ships"
                2 -> "perm-registration-requests/get-my-ships"
                7 -> "deletion-requests/get-my-ships"
                4 -> "mortgage-request/get-my-ships"
                5 -> "mortgage-redemption-request/get-my-ships"
                3 -> "ship-navigation-license-request/get-my-ships"
                6 -> "navigation-license-renewal-request/get-my-ships"
                8 -> "inspection-requests/get-my-ships"
                15 -> "inspection-requests/get-my-ships"
                else -> {
                    println("⚠️ Unknown requestTypeId: $requestTypeInt, using mortgage endpoint as fallback")
                    "mortgage-request/get-my-ships"
                }
            }

            val endpoint = "$baseEndpoint$filterParam"
            println("📡 Full API Call: $endpoint")
            println("   Request Type: $requestTypeInt → $baseEndpoint")

            when (val response = repo.onGet(endpoint)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Response received")
                    println("📄 Response JSON: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        val success = responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
                        println("📊 Status Code: $statusCode, Success: $success")

                        if (statusCode == 200 && success) {
                            val data = responseJson.jsonObject.getValue("data").jsonObject

                            // ✅ NEW API Structure: activeCoreShips[] and nonActiveCoreShip[]
                            val activeCoreShips = data["activeCoreShips"]?.jsonArray ?: emptyList()
                            val nonActiveCoreShips = data["nonActiveCoreShip"]?.jsonArray ?: emptyList()

                            println("📦 Active ships: ${activeCoreShips.size}")
                            println("📦 Non-active ships: ${nonActiveCoreShips.size}")

                            // ✅ FIXED: Parse active ships using the OUTER id from activeCoreShips
                            val activeShips = activeCoreShips.mapNotNull { shipItem ->
                                try {
                                    // Each item has: id, ship{}, isCurrent, shipInfoEngines[], shipInfoOwners[]
                                    val outerShipItemObject = shipItem.jsonObject
                                    val outerShipId = outerShipItemObject["id"]?.jsonPrimitive?.content
                                    val shipObject = outerShipItemObject.getValue("ship").jsonObject

                                    // ✅ Parse the ship and override the ID with the outer ID
                                    val marineUnit = parseMarineUnit(shipObject)

                                    // ✅ Override the ship.id with the outer activeCoreShips[].id
                                    if (outerShipId != null) {
                                        marineUnit.copy(id = outerShipId, isActive = true)
                                    } else {
                                        marineUnit.copy(isActive = true)
                                    }
                                } catch (e: Exception) {
                                    println("⚠ Failed to parse active ship: ${e.message}")
                                    e.printStackTrace()
                                    null
                                }
                            }

                            // ✅ FIXED: Parse non-active ships using the OUTER id
                            val nonActiveShips = nonActiveCoreShips.mapNotNull { shipItem ->
                                try {
                                    val outerShipItemObject = shipItem.jsonObject
                                    val outerShipId = outerShipItemObject["id"]?.jsonPrimitive?.content
                                    val shipObject = outerShipItemObject.getValue("ship").jsonObject

                                    // ✅ Parse the ship and override the ID with the outer ID
                                    val marineUnit = parseMarineUnit(shipObject)

                                    // ✅ Override the ship.id with the outer nonActiveCoreShip[].id
                                    if (outerShipId != null) {
                                        marineUnit.copy(id = outerShipId, isActive = false)
                                    } else {
                                        marineUnit.copy(isActive = false)
                                    }
                                } catch (e: Exception) {
                                    println("⚠ Failed to parse non-active ship: ${e.message}")
                                    e.printStackTrace()
                                    null
                                }
                            }

                            // Combine both active and non-active ships
                            val allShips = activeShips + nonActiveShips

                            println("✅ Successfully fetched ${allShips.size} ships (${activeShips.size} active, ${nonActiveShips.size} non-active)")
                            Result.success(allShips)
                        } else {
                            println("❌ Service failed with status: $statusCode")
                            println("❌ Response body: $responseJson")
                            Result.failure(Exception("Service failed with status: $statusCode"))
                        }
                    } else {
                        println("❌ Empty response from server")
                        Result.failure(Exception("Empty response from server"))
                    }
                }

                is RepoServiceState.Error -> {
                    println("❌ API Error - Code: ${response.code}")
                    println("❌ Error message: ${response.error}")
                    println("❌ Error type: ${response.error?.javaClass?.name}")
                    println("❌ Full error object: $response")

                    // Try to parse error response if it's JSON
                    try {
                        val errorJson = response.error.toString()
                        println("❌ Error as string: $errorJson")
                    } catch (e: Exception) {
                        println("⚠ Could not stringify error: ${e.message}")
                    }

                    Result.failure(Exception("API Error ${response.code}: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in getMyShips: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to get ships: ${e.message}"))
          }
     }

    /**
     * ✅ Generic function to parse ship JSON object to MarineUnit model
     * 🔄 Shared by: getMyShips (temporary certificate) & getMortgagedShips (mortgage release)
     *
     * This function handles ship data from different API responses:
     * - Temporary Certificate: data.content[].coreShipsResDto
     * - Mortgaged Ships: data[] (direct array)
     */
    private fun parseMarineUnit(shipJson: kotlinx.serialization.json.JsonObject): MarineUnit {
        return MarineUnit(
            // Core Information
            id = shipJson["id"]?.jsonPrimitive?.content ?: "",
            shipName = shipJson["shipName"]?.jsonPrimitive?.content ?: "",
            imoNumber = shipJson["imoNumber"]?.jsonPrimitive?.content,
            callSign = shipJson["callSign"]?.jsonPrimitive?.content ?: "",
            mmsiNumber = shipJson["mmsiNumber"]?.jsonPrimitive?.content ?: "",
            officialNumber = shipJson["officialNumber"]?.jsonPrimitive?.content ?: "",

            // Registration
            portOfRegistry = PortOfRegistry(
                id = shipJson["portOfRegistry"]?.jsonObject?.get("id")?.jsonPrimitive?.content ?: ""
            ),
            firstRegistrationDate = shipJson["firstRegistrationDate"]?.jsonPrimitive?.content ?: "",
            requestSubmissionDate = shipJson["requestSubmissionDate"]?.jsonPrimitive?.content ?: "",
            isTemp = shipJson["isTemp"]?.jsonPrimitive?.content ?: "0",

            // Classification
            marineActivity = MarineActivity(
                id = shipJson["marineActivity"]?.jsonObject?.get("id")?.jsonPrimitive?.int ?: 0
            ),
            shipCategory = ShipCategory(
                id = shipJson["shipCategory"]?.jsonObject?.get("id")?.jsonPrimitive?.int ?: 0
            ),
            shipType = ShipType(
                id = shipJson["shipType"]?.jsonObject?.get("id")?.jsonPrimitive?.int ?: 0
            ),
            proofType = ProofType(
                id = shipJson["proofType"]?.jsonObject?.get("id")?.jsonPrimitive?.int ?: 0
            ),

            // Build Information
            buildCountry = BuildCountry(
                id = shipJson["buildCountry"]?.jsonObject?.get("id")?.jsonPrimitive?.content ?: ""
            ),
            buildMaterial = BuildMaterial(
                id = shipJson["buildMaterial"]?.jsonObject?.get("id")?.jsonPrimitive?.int ?: 0
            ),
            shipBuildYear = shipJson["shipBuildYear"]?.jsonPrimitive?.content ?: "",
            buildEndDate = shipJson["buildEndDate"]?.jsonPrimitive?.content ?: "",
            shipYardName = shipJson["shipYardName"]?.jsonPrimitive?.content ?: "",

            // Tonnage & Capacity
            grossTonnage = shipJson["grossTonnage"]?.jsonPrimitive?.content ?: "",
            netTonnage = shipJson["netTonnage"]?.jsonPrimitive?.content ?: "",
            deadweightTonnage = shipJson["deadweightTonnage"]?.jsonPrimitive?.content ?: "",
            maxLoadCapacity = shipJson["maxLoadCapacity"]?.jsonPrimitive?.content ?: "",

            // Activity status (default to active when field missing)
            isActive = shipJson["isActive"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()?.let { it == 1 } ?: true
        )
    }

    /**
     * 🔒 Get mortgaged ships for owner (Mortgage Release Transaction)
     *
     * API: GET /ship/{ownerId}/owner-mortgaged-ships
     *
     * Response structure:
     * {
     *   "data": [ { ship1 }, { ship2 }, ... ]  ← Direct array of ships
     * }
     *
     * ⚠️ Different from getMyShips:
     * - getMyShips: Returns ALL ships in data.content[].coreShipsResDto (for temp certificate)
     * - getMortgagedShips: Returns ONLY mortgaged ships in data[] (for mortgage release)
     *
     * @param ownerId The owner ID (civil ID or commercial registration number)
     * @return Result with list of mortgaged ships ONLY
     */
    suspend fun getMortgagedShips(ownerId: String): Result<List<MarineUnit>> {
        return try {
            println("🔒 Fetching mortgaged ships for owner: $ownerId")

            val endpoint = "ship/$ownerId/owner-mortgaged-ships"
            println("📡 API Endpoint: $endpoint")

            when (val response = repo.onGet(endpoint)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Response received")
                    println("📄 Response JSON: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        val success = responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
                        println("📊 Status Code: $statusCode, Success: $success")

                        if (statusCode == 200 && success) {
                            // ✅ For mortgaged ships: data is a direct array (not nested in content)
                            val data = responseJson.jsonObject.getValue("data").jsonArray
                            println("📦 Mortgaged ships count: ${data.size}")

                            // Parse each ship using the same generic parser
                            val ships = data.mapNotNull { shipItem ->
                                try {
                                    parseMarineUnit(shipItem.jsonObject)
                                } catch (e: Exception) {
                                    println("⚠️ Failed to parse mortgaged ship: ${e.message}")
                                    e.printStackTrace()
                                    null
                                }
                            }

                            println("✅ Successfully parsed ${ships.size} mortgaged ships")
                            Result.success(ships)
                        } else {
                            println("❌ API failed with status: $statusCode")
                            Result.failure(Exception("API failed with status: $statusCode"))
                        }
                    } else {
                        println("❌ Empty response from server")
                        Result.failure(Exception("Empty response from server"))
                    }
                }

                is RepoServiceState.Error -> {
                    println("❌ API Error - Code: ${response.code}")
                    println("❌ Error: ${response.error}")
                    Result.failure(Exception("API Error ${response.code}: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in getMortgagedShips: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to get mortgaged ships: ${e.message}"))
        }
    }

    /**
     * Get ship details by ID (if needed separately)
     */
    suspend fun getShipById(shipId: String): Result<MarineUnit> {
        return try {
            when (val response = repo.onGet("user-profile/ships/$shipId")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        val success = responseJson.jsonObject.getValue("success").jsonPrimitive.boolean

                        if (statusCode == 200 && success) {
                            val data = responseJson.jsonObject.getValue("data").jsonObject
                            val ship = parseMarineUnit(data)
                            Result.success(ship)
                        } else {
                            Result.failure(Exception("Failed to get ship details"))
                        }
                    } else {
                        Result.failure(Exception("Empty response"))
                    }
                }

                is RepoServiceState.Error -> {
                    Result.failure(Exception("Failed to get ship: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to get ship: ${e.message}"))
        }
    }

    // =============================================================================================
    // 🔧 GENERIC TRANSACTION FUNCTIONS
    // =============================================================================================

    /**
     * ✅ Generic function to update any transaction status
     * PUT {endpoint}/{requestId}/update-status
     *
     * @param endpoint Base endpoint (e.g., "mortgage-request")
     * @param requestId The transaction request ID
     * @param statusId The new status ID
     * @param transactionType The transaction type name for logging
     * @param additionalData Optional additional data to send in request body
     * @return Result with success/failure
     */
    suspend fun updateTransactionStatus(
        endpoint: String,
        requestId: Int,
        statusId: Int,
        transactionType: String = "Transaction",
        additionalData: Map<String, Any> = emptyMap()
    ): Result<Boolean> {
        return try {
            println("=".repeat(80))
            println("🔄 Updating $transactionType status...")
            println("=".repeat(80))
            println("📤 Request Details:")
            println("   Endpoint: $endpoint/$requestId/update-status")
            println("   Status ID: $statusId")
            if (additionalData.isNotEmpty()) {
                println("   Additional Data: $additionalData")
            }

            // Create request body with statusId and any additional data
            val requestData = mutableMapOf<String, kotlinx.serialization.json.JsonElement>(
                "statusId" to kotlinx.serialization.json.JsonPrimitive(statusId)
            )

            // Add additional data if provided
            additionalData.forEach { (key, value) ->
                requestData[key] = when (value) {
                    is String -> kotlinx.serialization.json.JsonPrimitive(value)
                    is Int -> kotlinx.serialization.json.JsonPrimitive(value)
                    is Long -> kotlinx.serialization.json.JsonPrimitive(value)
                    is Double -> kotlinx.serialization.json.JsonPrimitive(value)
                    is Boolean -> kotlinx.serialization.json.JsonPrimitive(value)
                    else -> kotlinx.serialization.json.JsonPrimitive(value.toString())
                }
            }

            val requestBody = json.encodeToString(
                kotlinx.serialization.json.JsonObject.serializer(),
                kotlinx.serialization.json.JsonObject(requestData)
            )

            println("📤 Request Body: $requestBody")
            println("=".repeat(80))

            val fullEndpoint = "$endpoint/$requestId/update-status"

            when (val response = repo.onPutAuth(fullEndpoint, requestBody)) {
                is RepoServiceState.Success -> {
                    println("✅ $transactionType status updated successfully")
                    println("📥 Response: ${response.response}")
                    println("=".repeat(80))
                    Result.success(true)
                }
                is RepoServiceState.Error -> {
                    val errorMsg = "Failed to update $transactionType status (code: ${response.code})"
                    println("❌ $errorMsg")
                    println("=".repeat(80))
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception updating $transactionType status: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * ✅ Check inspection preview before sending permanent registration request
     * GET /api/v1/perm-registration-requests/{shipInfoId}/inspection-preview
     *
     * Response example:
     * {
     *   "message": "No valid inspection found for this request, inspection will be required to proceed",
     *   "statusCode": 200,
     *   "success": true,
     *   "timestamp": "2025-12-18 17:56:23",
     *   "data": 0
     * }
     *
     * @param shipInfoId The ship info ID
     * @return Result with inspection status (data field: 0 = no inspection, 1 = has inspection)
     */
    suspend fun checkInspectionPreview(
        id: Int,
        baseContext: String
    ): Result<Int> {
        return try {
            val endpoint = "$baseContext/$id/inspection-preview"
            println("   Endpoint: $endpoint")

            val response = repo.onGet(endpoint)

            when (response) {
                is RepoServiceState.Success -> {
                    println("✅ Inspection preview check successful")
                    println("📥 Response: ${response.response}")

                    // Parse the response
                    val responseObj = response.response.jsonObject
                    val message = responseObj["message"]?.jsonPrimitive?.content ?: ""
                    val success = responseObj["success"]?.jsonPrimitive?.boolean ?: false
                    val statusCode = responseObj["statusCode"]?.jsonPrimitive?.int ?: 0

                    // ✅ Parse data object to get needInspection
                    val dataObj = responseObj["data"]?.jsonObject
                    val needInspection = dataObj?.get("needInspection")?.jsonPrimitive?.boolean ?: false
                    val inspectionStatus = if (needInspection) 1 else 0

                    println("   Message: $message")
                    println("   Success: $success")
                    println("   Status Code: $statusCode")
                    println("   Need Inspection: $needInspection")
                    println("   Inspection Status: $inspectionStatus")
                    println("=".repeat(80))

                    if (success && statusCode == 200) {
                        Result.success(inspectionStatus)
                    } else {
                        val errorMsg = message.ifBlank { "Failed to check inspection preview" }
                        println("❌ $errorMsg")
                        Result.failure(Exception(errorMsg))
                    }
                }
                is RepoServiceState.Error -> {
                    val errorMsg = response.error ?: "Failed to check inspection preview (code: ${response.code})"
                    println("❌ $errorMsg")
                    println("=".repeat(80))
                    Result.failure(Exception(errorMsg as String?))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in checkInspectionPreview: ${e.message}")
            e.printStackTrace()
            println("=".repeat(80))
            Result.failure(e)
        }
    }

    /**
     * ✅ Send transaction request (used in review step for all transactions)
     * Generic method that works for any transaction type
     *
     * @param endpoint The transaction endpoint (e.g., "temporary-registration")
     * @param requestId The transaction request ID
     * @param transactionType The transaction type name for logging
     * @return ReviewResponse with message and needInspection flag
     */
    suspend fun sendTransactionRequest(
        endpoint: String,
        requestId: Int,
        transactionType: String = "Transaction",
        sendRequestPostOrPut: String
    ): com.informatique.mtcit.business.transactions.shared.ReviewResponse {
        return try {
            println("=".repeat(80))
            println("📤 Sending $transactionType Request...")
            println("=".repeat(80))
            println("   Request ID: $requestId")
            println("   HTTP Method: $sendRequestPostOrPut")

            // ✅ Fix: The endpoint already contains the full path with {requestId}/send-request
            // Just replace the {requestId} placeholder with the actual ID
            val fullEndpoint = endpoint.replace("{requestId}", requestId.toString())
            println("   Endpoint: $fullEndpoint")

            // ✅ Check sendRequestPostOrPut to determine which HTTP method to use
            val response = when (sendRequestPostOrPut.uppercase()) {
                "POST" -> {
                    println("   Using POST method")
                    repo.onPostAuth(fullEndpoint, "")
                }
                "PUT" -> {
                    println("   Using PUT method")
                    repo.onPutAuth(fullEndpoint, "")
                }
                else -> {
                    println("⚠️ Unknown HTTP method: $sendRequestPostOrPut, defaulting to POST")
                    repo.onPostAuth(fullEndpoint, "")
                }
            }

            when (response) {
                is RepoServiceState.Success -> {
                    println("✅ $transactionType request sent successfully")
                    println("📥 Response: ${response.response}")

                    // ✅ Parse response to extract message and needInspection flag
                    // Extract the `data` object safely from the response JSON
                    val dataObj = response.response.jsonObject["data"]?.jsonObject

                    // Parse needInspection: it may be a boolean or string in some responses
                    val needInspection = dataObj?.get("needInspection")?.jsonPrimitive?.let { prim ->
                        try {
                            prim.boolean
                        } catch (e: Exception) {
                            // Fallback: check textual content
                            prim.content.equals("true", ignoreCase = true)
                        }
                    } ?: false

                    // Parse message from response data if present, otherwise use defaults
                    val message = dataObj?.get("message")?.jsonPrimitive?.content ?:
                        if (needInspection) "تم إرسال الطلب بنجاح. في انتظار نتيجة الفحص الفني" else "تم إرسال الطلب بنجاح"

                    println("   Message: $message")
                    println("   Need Inspection: $needInspection")
                    println("=".repeat(80))

                    com.informatique.mtcit.business.transactions.shared.ReviewResponse(
                        message = message,
                        needInspection = needInspection,
                        additionalData = emptyMap<String, Any>()
                    )
                }
                is RepoServiceState.Error -> {
                    val errorCode = response.code ?: 0
                    val errorMsg: String = when (errorCode) {
                        401 -> "انتهت صلاحية الجلسة. الرجاء تحديث الرمز للمتابعة"
                        else -> response.error?.toString() ?: "Failed to send $transactionType request (code: $errorCode)"
                    }
                    println("❌ $errorMsg")
                    println("=".repeat(80))

                    // ✅ Throw ApiException with proper code so ViewModel can handle 401 specially
                    throw com.informatique.mtcit.common.ApiException(errorCode, errorMsg)
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in send$transactionType Request: ${e.message}")
            e.printStackTrace()
            println("=".repeat(80))

            // Re-throw to be handled by repository
            throw e
        }
    }

    /**
     * ✅ GENERIC: Proceed with request for a selected ship (works for ALL transactions)
     * POST {endpoint}/ship-info/{shipInfoId}/proceed-request
     *
     * This API is called when user selects a ship to validate and create initial request context.
     * If successful (statusCode 200), the flow continues to next step.
     * If error, the flow stops and shows error message.
     *
     * Usage Examples:
     * - Mortgage: /mortgage-request/ship-info/{shipInfoId}/proceed-request
     * - Deletion: /deletion-requests/ship-info/{shipInfoId}/proceed-request
     * - Registration: /registration-requests/ship-info/{shipInfoId}/proceed-request
     * - Permanent: /perm-registration-requests/ship-info/{shipInfoId}/proceed-request
     * - Redemption: /mortgage-redemption-request/ship-info/{shipInfoId}/proceed-request
     *
     * Response example:
     * {
     *   "message": "Add Successfully",
     *   "statusCode": 200,
     *   "success": true,
     *   "timestamp": "2025-12-21 12:58:10",
     *   "data": {
     *     "id": 1148,
     *     "shipInfo": { ... },
     *     "requestSerial": 862,
     *     "requestYear": 2025,
     *     "requestType": { "id": 1, ... },
     *     "status": { "id": 1, ... }
     *   }
     * }
     *
     * @param endpoint The base endpoint (will append /ship-info/{shipInfoId}/proceed-request)
     * @param shipInfoId The selected ship info ID
     * @param transactionType The transaction type name for logging (e.g., "Mortgage", "Deletion")
     * @return Result with the created request data (contains request ID)
     */
    suspend fun proceedWithRequest(
        endpoint: String,
        shipInfoId: String,
        transactionType: String = "Transaction"
    ): Result<ProceedRequestResponse> {
        return try {
            println("=".repeat(80))
            println("🚢 MarineUnitsApiService: Proceeding with $transactionType request for ship...")
            println("=".repeat(80))
            println("   Ship Info ID (raw): $shipInfoId")
            println("   Base Endpoint: $endpoint")

            // ✅ Clean the shipInfoId - remove quotes, brackets, and whitespace
            // Convert to pure integer string (e.g., "64", "[64]", "\"64\"" all become "64")
            val cleanShipId = shipInfoId
                .trim()
                .removeSurrounding("\"")  // Remove quotes if present
                .removeSurrounding("[", "]")  // Remove brackets if present
                .trim()

            println("   Ship Info ID (cleaned): $cleanShipId")

            // ✅ Build the full endpoint by replacing {shipInfoId} placeholder
            // If endpoint already contains the pattern, just replace the placeholder
            // Otherwise, append /ship-info/{shipInfoId}/proceed-request
            val fullEndpoint = if (endpoint.contains("ship-info/{shipInfoId}/proceed-request")) {
                endpoint.replace("{shipInfoId}", cleanShipId)
            } else {
                "$endpoint/ship-info/$cleanShipId/proceed-request"
            }

            println("   Full Endpoint: $fullEndpoint")
            println("=".repeat(80))

            when (val response = repo.onPostAuth(fullEndpoint, "")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Response received")
                    println("📄 Response JSON: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        val success = responseJson.jsonObject["success"]?.jsonPrimitive?.content == "true"

                        println("📊 Status Code: $statusCode, Success: $success")

                        if (statusCode == 200 && success) {
                            // Parse the response
                            val proceedResponse = json.decodeFromJsonElement<ProceedRequestResponse>(responseJson)

                            println("✅ Proceed request successful!")
                            println("   Transaction Type: $transactionType")
                            println("   Request ID: ${proceedResponse.data.id}")
                            println("   Request Serial: ${proceedResponse.data.requestSerial}")
                            println("   Request Year: ${proceedResponse.data.requestYear}")
                            println("   Ship Info ID: ${proceedResponse.data.shipInfo?.id}")
                            println("=".repeat(80))

                            Result.success(proceedResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "فشل في متابعة طلب $transactionType"
                            println("❌ API returned error: $message (Status: $statusCode)")
                            println("=".repeat(80))
                            Result.failure(Exception(message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        println("=".repeat(80))
                        Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    // ✅ Extract error message from response body if available
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    val errorMsg = if (errorMessage.isNotBlank() && errorMessage != "Unknown error") {
                        errorMessage
                    } else {
                        // ✅ Special message for 401 errors
                        if (response.code == 401) {
                            "انتهت صلاحية الجلسة. الرجاء تحديث الرمز للمتابعة"
                        } else {
                            "فشل في متابعة طلب $transactionType (code: ${response.code})"
                        }
                    }

                    println("❌ $errorMsg")
                    println("   HTTP Code: ${response.code}")
                    println("   Error Body: ${response.error}")
                    println("=".repeat(80))

                    // ✅ CRITICAL FIX: Throw ApiException with status code so BaseTransactionViewModel can detect 401
                    throw com.informatique.mtcit.common.ApiException(response.code, errorMsg)
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in proceedWithRequest ($transactionType): ${e.message}")
            e.printStackTrace()
            println("=".repeat(80))
            Result.failure(Exception("Failed to proceed with $transactionType request: ${e.message}"))
        }
    }
    /**
     * Add maritime identification (IMO, MMSI, Call Sign) to a ship
     * PATCH /api/v1/perm-registration-requests/{shipId}/add-ship-identity
     *
     * This endpoint is used only when the ship is missing one or more of these fields.
     * If the ship already has all three fields, this step should not appear.
     *
     * @param shipId The ship info ID
     * @param imoNumber IMO number (only if missing)
     * @param mmsiNumber MMSI number (only if missing)
     * @param callSign Call sign (only if missing)
     * @return Result with response or error
     */
    suspend fun addMaritimeIdentity(
        shipId: Int,
        imoNumber: String?,
        mmsiNumber: String?,
        callSign: String?
    ): Result<com.informatique.mtcit.data.model.MaritimeIdentityResponse> {
        return try {
            println("=".repeat(80))
            println("🚢 Adding Maritime Identity to Ship")
            println("   Ship ID: $shipId")
            println("   IMO Number: ${imoNumber ?: "(not provided)"}")
            println("   MMSI Number: ${mmsiNumber ?: "(not provided)"}")
            println("   Call Sign: ${callSign ?: "(not provided)"}")

            val endpoint = "perm-registration-requests/$shipId/add-ship-identity"

            // Build request body with only non-null fields
            val requestBody = com.informatique.mtcit.data.model.MaritimeIdentityRequest(
                imoNumber = imoNumber,
                mmsiNumber = mmsiNumber,
                callSign = callSign
            )

            // Serialize to JSON
            val jsonBody = json.encodeToString(
                com.informatique.mtcit.data.model.MaritimeIdentityRequest.serializer(),
                requestBody
            )

            println("📤 PATCH Request:")
            println("   Endpoint: $endpoint")
            println("   Body: $jsonBody")

            // Make PATCH request
            val response = repo.onPatchAuth(endpoint, requestBody)

            when (response) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("📥 Response received:")
                    println(responseJson.toString())

                    // Parse response
                    val maritimeResponse = json.decodeFromJsonElement(
                        com.informatique.mtcit.data.model.MaritimeIdentityResponse.serializer(),
                        responseJson
                    )

                    if (maritimeResponse.success && maritimeResponse.statusCode in 200..201) {
                        println("✅ Maritime identity added successfully!")
                        println("   Updated Ship ID: ${maritimeResponse.data?.id}")
                        println("=".repeat(80))
                        Result.success(maritimeResponse)
                    } else {
                        val message = maritimeResponse.message.ifBlank { "Failed to add maritime identity" }
                        println("❌ API returned error: $message")
                        println("=".repeat(80))
                        Result.failure(Exception(message))
                    }
                }
                is RepoServiceState.Error -> {
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    val errorMsg = if (errorMessage.isNotBlank() && errorMessage != "Unknown error") {
                        errorMessage
                    } else {
                        "Failed to add maritime identity (code: ${response.code})"
                    }

                    println("❌ $errorMsg")
                    println("   HTTP Code: ${response.code}")
                    println("   Error Body: ${response.error}")
                    println("=".repeat(80))
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in addMaritimeIdentity: ${e.message}")
            e.printStackTrace()
            println("=".repeat(80))
            Result.failure(Exception("Failed to add maritime identity: ${e.message}"))
        }
    }
}

