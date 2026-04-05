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
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ✅ Pagination result for a single page of ships (used by infinite scroll)
 */
data class ShipsPage(
    val ships: List<com.informatique.mtcit.business.transactions.shared.MarineUnit>,
    val currentPage: Int,
    val totalPages: Int,
    val isLastPage: Boolean
)

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

            println("🔍 Fetching ships with filter: $filterJson")
            println("📋 Base64 encoded filter: $base64Filter")

            // ✅ All transaction types now use the unified coreshipinfo/get-my-ships endpoint.
            // The response is always paginated with a Spring Page structure:
            // { data: { content: [...], totalPages: N, totalElements: N, last: bool, ... } }
            // URL shape: coreshipinfo/get-my-ships?filter=BASE64&page=0&size=50
            val baseEndpoint = "coreshipinfo/get-my-ships"
            val pageSize = 5 // fetch up to 50 ships per page to minimise round-trips

            // ── Helper: fetch a single page and parse content[] ──────────────────────────
            fun buildPageEndpoint(page: Int): String =
                "$baseEndpoint?filter=$base64Filter&page=$page&size=$pageSize"

            fun parseOnePage(responseJson: kotlinx.serialization.json.JsonElement): Pair<List<MarineUnit>, Int> {
                val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                val success = responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
                println("📊 Status Code: $statusCode, Success: $success")

                if (statusCode != 200 || !success) {
                    throw Exception("Service failed with status: $statusCode")
                }

                val data = responseJson.jsonObject.getValue("data").jsonObject
                val contentArray = data["content"]?.jsonArray
                    ?: throw Exception("Expected 'content' array in response data")

                val totalPages = data["totalPages"]?.jsonPrimitive?.int ?: 1
                val pageNumber = data["number"]?.jsonPrimitive?.int ?: 0

                println("📦 content[] size: ${contentArray.size}, page: $pageNumber / totalPages: $totalPages")

                val ships = contentArray.mapNotNull { shipItem ->
                    try {
                        parseContentShipItem(shipItem.jsonObject)
                    } catch (e: Exception) {
                        println("⚠ Failed to parse ship from content[]: ${e.message}")
                        e.printStackTrace()
                        null
                    }
                }
                return Pair(ships, totalPages)
            }
            // ─────────────────────────────────────────────────────────────────────────────

            // Fetch page 0 first
            println("📡 Full API Call (page 0): ${buildPageEndpoint(0)}")
            val firstResponse = repo.onGet(buildPageEndpoint(0))

            when (firstResponse) {
                is RepoServiceState.Success -> {
                    val responseJson = firstResponse.response
                    println("✅ API Response (page 0) received")
                    println("📄 Response JSON: $responseJson")

                    if (responseJson.jsonObject.isEmpty()) {
                        println("❌ Empty response from server")
                        return Result.failure(Exception("Empty response from server"))
                    }

                    val (firstPageShips, totalPages) = parseOnePage(responseJson)
                    val allShips = mutableListOf<MarineUnit>()
                    allShips.addAll(firstPageShips)

                    // Fetch remaining pages (1 … totalPages-1) if any
                    if (totalPages > 1) {
                        println("📄 Paginated response: $totalPages pages total — fetching remaining pages...")
                        for (page in 1 until totalPages) {
                            println("📡 Fetching page $page / ${totalPages - 1}")
                            val pageResponse = repo.onGet(buildPageEndpoint(page))
                            when (pageResponse) {
                                is RepoServiceState.Success -> {
                                    val (pageShips, _) = parseOnePage(pageResponse.response)
                                    allShips.addAll(pageShips)
                                    println("   ✅ Page $page: ${pageShips.size} ships added")
                                }
                                is RepoServiceState.Error -> {
                                    println("⚠ Failed to fetch page $page (code: ${pageResponse.code}): ${pageResponse.error} — skipping")
                                }
                            }
                        }
                    }

                    println("✅ Successfully fetched ${allShips.size} ships total (across $totalPages page(s))")
                    Result.success(allShips)
                }

                is RepoServiceState.Error -> {
                    println("❌ API Error - Code: ${firstResponse.code}")
                    println("❌ Error message: ${firstResponse.error}")
                    println("❌ Error type: ${firstResponse.error?.javaClass?.name}")
                    println("❌ Full error object: $firstResponse")

                    try {
                        val errorJson = firstResponse.error.toString()
                        println("❌ Error as string: $errorJson")
                    } catch (e: Exception) {
                        println("⚠ Could not stringify error: ${e.message}")
                    }

                    Result.failure(Exception("API Error ${firstResponse.code}: ${firstResponse.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in getMyShips: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to get ships: ${e.message}"))
        }
    }

    /**
     * ✅ Parse a single item from the unified paginated content[] response.
     *
     * New flat structure (all transaction types via coreshipinfo/get-my-ships):
     * {
     *   "shipInfoId": 3780,
     *   "shipName": "magal2",
     *   "registrationNumber": "MTCIT-KHS-2026-507",
     *   "officialNumber": "2622",
     *   "portOfRegistry": "PORT OF KHASSAB",   ← plain String, NOT an object
     *   "marineActivityName": "Coastal Fishing",
     *   "isActive": true,
     *   "callSign": "678768"                   ← optional
     * }
     */
    private fun parseContentShipItem(shipObject: kotlinx.serialization.json.JsonObject): MarineUnit {
        val shipInfoId = shipObject["shipInfoId"]?.jsonPrimitive?.content ?: ""
        val shipName = shipObject["shipName"]?.jsonPrimitive?.content ?: ""
        val officialNumber = shipObject["officialNumber"]?.jsonPrimitive?.content ?: ""
        // portOfRegistry is a plain String in the new unified response (e.g. "PORT OF KHASSAB")
        // Store as both id and nameAr so the computed property can display it correctly
        val portOfRegistryStr = shipObject["portOfRegistry"]?.jsonPrimitive?.content ?: ""
        val marineActivityName = shipObject["marineActivityName"]?.jsonPrimitive?.content ?: ""
        val isActive = shipObject["isActive"]?.jsonPrimitive?.boolean ?: true
        val callSign = shipObject["callSign"]?.jsonPrimitive?.content ?: ""

        return MarineUnit(
            id = shipInfoId,
            shipName = shipName,
            marineActivityName = marineActivityName,
            officialNumber = officialNumber,
            // ✅ Store the plain-string port name in both id and nameAr fields so the
            //    computed `registrationPort` property can display it correctly
            portOfRegistry = PortOfRegistry(id = portOfRegistryStr, nameAr = portOfRegistryStr),
            marineActivity = MarineActivity(id = 0), // ID not present in this response
            isActive = isActive,
            callSign = callSign,
            // Fields not present in this response — use safe defaults
            imoNumber = null,
            mmsiNumber = "",
            firstRegistrationDate = "",
            requestSubmissionDate = "",
            isTemp = "0",
            shipCategory = ShipCategory(id = 0),
            shipType = ShipType(id = 0),
            proofType = ProofType(id = 0),
            buildCountry = BuildCountry(id = ""),
            buildMaterial = BuildMaterial(id = 0),
            shipBuildYear = "",
            buildEndDate = "",
            grossTonnage = "",
            netTonnage = "",
            deadweightTonnage = "",
            maxLoadCapacity = "",
            totalLength = "",
            totalWidth = "",
            draft = "",
            height = "",
            numberOfDecks = ""
        )
    }

    /**
     * ✅ NEW: Fetch a single page of ships for infinite scroll
     *
     * Returns a [ShipsPage] containing the ships on that page plus pagination metadata.
     * Callers should keep requesting pages (incrementing [page]) until [ShipsPage.isLastPage] is true.
     *
     * @param ownerCivilId   Civil ID of the owner (required)
     * @param commercialRegNumber CR Number for companies (optional)
     * @param requestTypeId  Transaction type ID for filtering
     * @param page           Zero-based page index to fetch
     * @param pageSize       Number of items per page (default 5)
     */
    suspend fun getMyShipsPage(
        ownerCivilId: String?,
        commercialRegNumber: String? = null,
        requestTypeId: String? = null,
        page: Int = 0,
        pageSize: Int = 5
    ): Result<ShipsPage> {
        return try {
            val requestTypeInt = requestTypeId?.toIntOrNull() ?: 0
            val commercialNumberForFilter = commercialRegNumber?.takeIf { it.isNotBlank() }
            val ownerIdForFilter = ownerCivilId?.takeIf { it.isNotBlank() } ?: ""

            if (ownerIdForFilter.isBlank()) {
                println("❌ getMyShipsPage: No ownerId provided (required)")
                return Result.failure(IllegalArgumentException("ownerId is required"))
            }

            val filterJsonElement = kotlinx.serialization.json.buildJsonObject {
                put("requestTypeId", kotlinx.serialization.json.JsonPrimitive(requestTypeInt))
                if (commercialNumberForFilter != null) {
                    put("commercialNumber", kotlinx.serialization.json.JsonPrimitive(commercialNumberForFilter))
                }
                put("ownerId", kotlinx.serialization.json.JsonPrimitive(ownerIdForFilter))
            }

            val filterJson = filterJsonElement.toString()
            val base64Filter = Base64.encodeToString(
                filterJson.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )

            val endpoint = "coreshipinfo/get-my-ships?filter=$base64Filter&page=$page&size=$pageSize"
            println("📡 getMyShipsPage: fetching page=$page size=$pageSize → $endpoint")

            when (val response = repo.onGet(endpoint)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (responseJson.jsonObject.isEmpty()) {
                        return Result.failure(Exception("Empty response from server"))
                    }

                    val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                    val success = responseJson.jsonObject.getValue("success").jsonPrimitive.boolean

                    if (statusCode != 200 || !success) {
                        return Result.failure(Exception("Service failed with status: $statusCode"))
                    }

                    val data = responseJson.jsonObject.getValue("data").jsonObject
                    val contentArray = data["content"]?.jsonArray
                        ?: return Result.failure(Exception("Expected 'content' array in response data"))

                    val totalPages = data["totalPages"]?.jsonPrimitive?.int ?: 1
                    val isLast = data["last"]?.jsonPrimitive?.boolean ?: (page >= totalPages - 1)

                    val ships = contentArray.mapNotNull { shipItem ->
                        try {
                            parseContentShipItem(shipItem.jsonObject)
                        } catch (e: Exception) {
                            println("⚠ Failed to parse ship from content[]: ${e.message}")
                            null
                        }
                    }

                    println("✅ getMyShipsPage: page=$page ships=${ships.size} totalPages=$totalPages isLast=$isLast")
                    Result.success(ShipsPage(ships = ships, currentPage = page, totalPages = totalPages, isLastPage = isLast))
                }

                is RepoServiceState.Error -> {
                    println("❌ getMyShipsPage error code=${response.code}: ${response.error}")
                    Result.failure(Exception("API Error ${response.code}: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in getMyShipsPage: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to get ships page: ${e.message}"))
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
        // Helper to extract nameAr from a nested object
        fun nameAr(key: String) = shipJson[key]?.jsonObject?.get("nameAr")?.jsonPrimitive?.content ?: ""
        fun nameArObj(obj: kotlinx.serialization.json.JsonObject?) = obj?.get("nameAr")?.jsonPrimitive?.content ?: ""

        return MarineUnit(
            // Core Information
            id = shipJson["id"]?.jsonPrimitive?.content ?: "",
            shipName = shipJson["shipName"]?.jsonPrimitive?.content ?: "",
            imoNumber = shipJson["imoNumber"]?.jsonPrimitive?.content,
            callSign = shipJson["callSign"]?.jsonPrimitive?.content ?: "",
            mmsiNumber = shipJson["mmsiNumber"]?.jsonPrimitive?.content ?: "",
            officialNumber = shipJson["officialNumber"]?.jsonPrimitive?.content ?: "",
            // ✅ Extract nameAr from nested portOfRegistry object
            marineActivityName = nameAr("marineActivity"),

            // Registration
            portOfRegistry = PortOfRegistry(
                id = shipJson["portOfRegistry"]?.jsonObject?.get("id")?.jsonPrimitive?.content ?: "",
                nameAr = nameAr("portOfRegistry"),
                nameEn = shipJson["portOfRegistry"]?.jsonObject?.get("nameEn")?.jsonPrimitive?.content ?: ""
            ),
            firstRegistrationDate = shipJson["firstRegistrationDate"]?.jsonPrimitive?.content ?: "",
            requestSubmissionDate = shipJson["requestSubmissionDate"]?.jsonPrimitive?.content ?: "",
            isTemp = shipJson["isTemp"]?.jsonPrimitive?.content ?: "0",

            // Classification
            marineActivity = MarineActivity(
                id = shipJson["marineActivity"]?.jsonObject?.get("id")?.jsonPrimitive?.int ?: 0,
                nameAr = nameAr("marineActivity"),
                nameEn = shipJson["marineActivity"]?.jsonObject?.get("nameEn")?.jsonPrimitive?.content ?: ""
            ),
            shipCategory = ShipCategory(
                id = shipJson["shipCategory"]?.jsonObject?.get("id")?.jsonPrimitive?.int ?: 0,
                nameAr = nameAr("shipCategory"),
                nameEn = shipJson["shipCategory"]?.jsonObject?.get("nameEn")?.jsonPrimitive?.content ?: ""
            ),
            shipType = ShipType(
                id = shipJson["shipType"]?.jsonObject?.get("id")?.jsonPrimitive?.int ?: 0,
                nameAr = nameAr("shipType"),
                nameEn = shipJson["shipType"]?.jsonObject?.get("nameEn")?.jsonPrimitive?.content ?: ""
            ),
            proofType = ProofType(
                id = shipJson["proofType"]?.jsonObject?.get("id")?.jsonPrimitive?.int ?: 0,
                nameAr = nameAr("proofType"),
                nameEn = shipJson["proofType"]?.jsonObject?.get("nameEn")?.jsonPrimitive?.content ?: ""
            ),

            // Build Information
            buildCountry = BuildCountry(
                id = shipJson["buildCountry"]?.jsonObject?.get("id")?.jsonPrimitive?.content ?: "",
                nameAr = nameAr("buildCountry"),
                nameEn = shipJson["buildCountry"]?.jsonObject?.get("nameEn")?.jsonPrimitive?.content ?: ""
            ),
            buildMaterial = BuildMaterial(
                id = shipJson["buildMaterial"]?.jsonObject?.get("id")?.jsonPrimitive?.int ?: 0,
                nameAr = nameAr("buildMaterial"),
                nameEn = shipJson["buildMaterial"]?.jsonObject?.get("nameEn")?.jsonPrimitive?.content ?: ""
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

    /**
     * ✅ Get full ship core info by shipInfoId
     * API: GET /coreshipinfo/ship/{id}
     *
     * Returns complete details including engines, owners, and certifications.
     * Called when user taps "عرض جميع البيانات" in the marine unit selector.
     */
    suspend fun getShipCoreInfo(shipInfoId: String): Result<com.informatique.mtcit.business.transactions.shared.CoreShipInfo> {
        return try {
            val endpoint = "coreshipinfo/ship/$shipInfoId"
            println("📡 getShipCoreInfo: fetching $endpoint")

            when (val response = repo.onGet(endpoint)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("📄 getShipCoreInfo response: $responseJson")

                    val statusCode = responseJson.jsonObject["statusCode"]?.jsonPrimitive?.int ?: 0
                    val success = responseJson.jsonObject["success"]?.jsonPrimitive?.boolean ?: false

                    if (statusCode == 200 && success) {
                        val data = responseJson.jsonObject["data"]?.jsonObject
                            ?: return Result.failure(Exception("Missing data in response"))

                        val shipInfoId2 = data["id"]?.jsonPrimitive?.int ?: 0
                        val shipObj = data["ship"]?.jsonObject

                        fun str(obj: kotlinx.serialization.json.JsonObject?, key: String) =
                            obj?.get(key)?.jsonPrimitive?.contentOrNull ?: ""
                        fun nameAr(obj: kotlinx.serialization.json.JsonObject?, key: String) =
                            obj?.get(key)?.jsonObject?.get("nameAr")?.jsonPrimitive?.contentOrNull ?: ""

                        // Parse engines
                        val engines = data["shipInfoEngines"]?.jsonArray?.mapNotNull { item ->
                            try {
                                val eng = item.jsonObject["engine"]?.jsonObject ?: return@mapNotNull null
                                com.informatique.mtcit.business.transactions.shared.CoreEngineInfo(
                                    serialNumber = str(eng, "engineSerialNumber"),
                                    engineType = eng["engineType"]?.jsonObject?.get("nameAr")?.jsonPrimitive?.contentOrNull ?: "",
                                    enginePower = eng["enginePower"]?.jsonPrimitive?.contentOrNull ?: "",
                                    engineStatus = eng["engineStatus"]?.jsonObject?.get("nameAr")?.jsonPrimitive?.contentOrNull ?: ""
                                )
                            } catch (e: Exception) { null }
                        } ?: emptyList()

                        // Parse owners
                        val owners = data["shipInfoOwners"]?.jsonArray?.mapNotNull { item ->
                            try {
                                val own = item.jsonObject["owner"]?.jsonObject ?: return@mapNotNull null
                                com.informatique.mtcit.business.transactions.shared.CoreOwnerInfo(
                                    ownerName = str(own, "ownerName"),
                                    ownerCivilId = str(own, "ownerCivilId"),
                                    ownershipPercentage = own["ownershipPercentage"]?.jsonPrimitive?.double ?: 0.0,
                                    isRepresentative = own["isRepresentative"]?.jsonPrimitive?.int == 1
                                )
                            } catch (e: Exception) { null }
                        } ?: emptyList()

                        // Parse certifications
                        val certs = data["shipCertifications"]?.jsonArray?.mapNotNull { item ->
                            try {
                                val cert = item.jsonObject
                                com.informatique.mtcit.business.transactions.shared.CoreCertificationInfo(
                                    certificationNumber = str(cert, "certificationNumber"),
                                    issuedDate = str(cert, "issuedDate"),
                                    expiryDate = str(cert, "expiryDate"),
                                    certificationType = cert["certificationType"]?.jsonObject?.get("nameAr")?.jsonPrimitive?.contentOrNull ?: ""
                                )
                            } catch (e: Exception) { null }
                        } ?: emptyList()

                        val info = com.informatique.mtcit.business.transactions.shared.CoreShipInfo(
                            shipInfoId = shipInfoId2,
                            shipName = str(shipObj, "shipName").ifEmpty { str(shipObj, "name") },
                            // callSign and imoNumber: try ship object first, then data level as fallback
                            imoNumber = str(shipObj, "imoNumber").ifEmpty { str(data, "imoNumber") },
                            callSign = str(shipObj, "callSign").ifEmpty { str(data, "callSign") },
                            officialNumber = str(shipObj, "officialNumber").ifEmpty { str(data, "officialNumber") },
                            registrationNumber = str(shipObj, "registrationNumber"),
                            portOfRegistry = nameAr(shipObj, "portOfRegistry"),
                            marineActivity = nameAr(shipObj, "marineActivity"),
                            shipCategory = nameAr(shipObj, "shipCategory"),
                            shipType = nameAr(shipObj, "shipType"),
                            buildMaterial = nameAr(shipObj, "buildMaterial"),
                            shipBuildYear = shipObj?.get("shipBuildYear")?.jsonPrimitive?.contentOrNull ?: "",
                            buildEndDate = str(shipObj, "buildEndDate"),
                            grossTonnage = shipObj?.get("grossTonnage")?.jsonPrimitive?.contentOrNull ?: "",
                            netTonnage = shipObj?.get("netTonnage")?.jsonPrimitive?.contentOrNull ?: "",
                            vesselLengthOverall = shipObj?.get("vesselLengthOverall")?.jsonPrimitive?.contentOrNull ?: "",
                            vesselBeam = shipObj?.get("vesselBeam")?.jsonPrimitive?.contentOrNull ?: "",
                            vesselDraft = shipObj?.get("vesselDraft")?.jsonPrimitive?.contentOrNull ?: "",
                            isTemp = shipObj?.get("isTemp")?.jsonPrimitive?.int == 1,
                            engines = engines,
                            owners = owners,
                            certifications = certs
                        )
                        println("✅ getShipCoreInfo success: ${info.shipName}")
                        Result.success(info)
                    } else {
                        Result.failure(Exception("Service failed with status: $statusCode"))
                    }
                }
                is RepoServiceState.Error -> {
                    Result.failure(Exception("API Error ${response.code}: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in getShipCoreInfo: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to get ship core info: ${e.message}"))
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

                    // ✅ Parse data field - can be either a primitive (0, 1) or an object
                    val inspectionStatus = try {
                        // Try as primitive first (data: 0 or data: 1)
                        responseObj["data"]?.jsonPrimitive?.int ?: 0
                    } catch (e: Exception) {
                        // If fails, try as object with needInspection field
                        try {
                            val dataObj = responseObj["data"]?.jsonObject
                            val needInspection = dataObj?.get("needInspection")?.jsonPrimitive?.boolean ?: false
                            // ✅ FIXED: needInspection=true means status=0 (needs inspection)
                            //           needInspection=false means status=1 (has inspection)
                            if (needInspection) 0 else 1
                        } catch (e2: Exception) {
                            // Default to 0 (needs inspection) if parsing fails
                            println("⚠️ Could not parse inspection data, defaulting to 0 (needs inspection)")
                            0
                        }
                    }

                    println("   Message: $message")
                    println("   Success: $success")
                    println("   Status Code: $statusCode")
                    println("   Inspection Status: $inspectionStatus (0=needs inspection, 1=has inspection)")
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

                    // ✅ Parse response - handle both JsonObject and JsonPrimitive (string) in data field
                    val dataElement = response.response.jsonObject["data"]
                    println("🔍 Data element type: ${dataElement?.javaClass?.simpleName}")

                    // Try to parse as JsonObject first (for complex responses)
                    val dataObj = try {
                        when {
                            dataElement == null -> {
                                println("⚠️ Data element is null")
                                null
                            }
                            dataElement is kotlinx.serialization.json.JsonObject -> {
                                println("✅ Data is JsonObject")
                                dataElement
                            }
                            else -> {
                                println("ℹ️ Data is not JsonObject (type: ${dataElement.javaClass.simpleName}), treating as simple response")
                                null
                            }
                        }
                    } catch (e: Exception) {
                        println("⚠️ Exception parsing data element: ${e.message}")
                        null
                    }

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
                    val message = dataObj?.get("message")?.jsonPrimitive?.content
                        ?: response.response.jsonObject["message"]?.jsonPrimitive?.content
                        ?: if (needInspection) "تم إرسال الطلب بنجاح. في انتظار نتيجة الفحص الفني" else "تم إرسال الطلب بنجاح"

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

