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
     * - For individuals (after PKI auth): send ownerCivilId
     * - For companies (after PKI auth): send commercialRegNumber
     *
     * Behavior changes:
     * - The API call will NOT be performed unless `stepActive` is true. This prevents
     *   automatic network calls when the transaction flow hasn't reached the ships step.
     * - Pass `ownerCivilId` for individuals or `commercialRegNumber` for companies.
     * - For local testing you can set `useTestCivilId = true` to use a fixed civil ID.
     *   When `useTestCivilId` is true, the call will be executed even if `stepActive` is false
     *   so you can test without changing callers everywhere.
     */
    suspend fun getMyShips(
        ownerCivilId: String? = null,
        commercialRegNumber: String? = null,
        stepActive: Boolean = false,
        useTestCivilId: Boolean = false
    ): Result<List<MarineUnit>> {
        return try {
            // If the step is not active, don't call the API unless test-mode override is enabled.
            if (!stepActive && !useTestCivilId) {
                println("⏸️ getMyShips: call suppressed because stepActive=false. Provide stepActive=true when you want to fetch ships.")
                return Result.failure(IllegalStateException("getMyShips suppressed: step not active"))
            }

            if (!stepActive && useTestCivilId) {
                println("🧪 getMyShips: stepActive is false but useTestCivilId=true => forcing call with test civil id")
            }

            // ✅ Determine which identifier to send. Priority:
            // 1) commercialRegNumber (if provided) - for companies
            // 2) ownerCivilId (if provided) - for individuals
            // 3) test civil id if useTestCivilId == true

            println("🔍 Input parameters:")
            println("   - ownerCivilId: $ownerCivilId")
            println("   - commercialRegNumber: $commercialRegNumber")
            println("   - stepActive: $stepActive")
            println("   - useTestCivilId: $useTestCivilId")

            // ✅ Build filter JSON depending on which identifier is present
            // Priority: commercialRegNumber first (for companies), then ownerCivilId (for individuals)
            val filterJson = when {
                !commercialRegNumber.isNullOrBlank() -> {
                    println("✅ Using commercialRegNumber for company")
                    """{"commercialRegNumber":"$commercialRegNumber"}"""
                }
                !ownerCivilId.isNullOrBlank() -> {
                    println("✅ Using ownerCivilId for individual")
                    """{"ownerCivilId":"$ownerCivilId"}"""
                }
                useTestCivilId -> {
                    println("✅ Using test civil ID")
                    """{"ownerCivilId":"12345678"}"""
                }
                else -> {
                    println("❌ No identifier provided")
                    return Result.failure(IllegalArgumentException("ownerCivilId or commercialRegNumber required"))
                }
            }.trimIndent()

            // Base64 encode the filter
            val base64Filter = Base64.encodeToString(
                filterJson.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )

            val filterParam = "?filter=$base64Filter"

            println("🔍 Fetching ships with filter: $filterJson")
            println("📋 Base64 encoded filter: $base64Filter")

            val endpoint = "api/v1/user-profile/getMyShips$filterParam"
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
                            val content = data.getValue("content").jsonArray
                            println("📦 Content array size: ${content.size}")

                            // Parse each ship item
                            val ships = content.mapNotNull { shipItem ->
                                try {
                                    val coreShipDto = shipItem.jsonObject.getValue("coreShipsResDto").jsonObject
                                    parseMarineUnit(coreShipDto)
                                } catch (e: Exception) {
                                    println("⚠️ Failed to parse ship: ${e.message}")
                                    e.printStackTrace()
                                    null
                                }
                            }

                            println("✅ Successfully fetched ${ships.size} ships")
                            Result.success(ships)
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
                        println("⚠️ Could not stringify error: ${e.message}")
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
     * Parse ship JSON object to MarineUnit model
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