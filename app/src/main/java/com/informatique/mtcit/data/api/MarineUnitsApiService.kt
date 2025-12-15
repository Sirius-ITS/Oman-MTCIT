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
import com.informatique.mtcit.di.module.AppRepository
import com.informatique.mtcit.di.module.RepoServiceState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
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

            if (!stepActive && useTestCivilId) {
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

            val endpoint = "api/v1/mortgage-request/get-my-ships$filterParam"
            println("📡 Full API Call: $endpoint")

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
                                        marineUnit.copy(id = outerShipId)
                                    } else {
                                        marineUnit
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
                                        marineUnit.copy(id = outerShipId)
                                    } else {
                                        marineUnit
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
        )
    }

    /**
     * 🔒 Get mortgaged ships for owner (Mortgage Release Transaction)
     *
     * API: GET /api/v1/ship/{ownerId}/owner-mortgaged-ships
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

            val endpoint = "api/v1/ship/$ownerId/owner-mortgaged-ships"
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
            when (val response = repo.onGet("api/v1/user-profile/ships/$shipId")) {
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
}