package com.informatique.mtcit.data.api

import com.informatique.mtcit.common.ApiException
import com.informatique.mtcit.common.ErrorMessageExtractor
import com.informatique.mtcit.data.dto.CrewReqDto
import com.informatique.mtcit.data.dto.CrewResDto
import com.informatique.mtcit.data.dto.NavigationAreaResDto
import com.informatique.mtcit.di.module.AppRepository
import com.informatique.mtcit.di.module.RepoServiceState
import io.ktor.http.content.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API Service for Navigation License Requests (Issue & Renew)
 * Follows the exact pattern as MarineUnitsApiService
 */
@Singleton
class NavigationLicenseApiService @Inject constructor(
    private val repo: AppRepository,
    private val json: Json
) {

    // ========================================
    // ISSUE NAVIGATION LICENSE
    // ========================================

    /**
     * Create a new ship navigation license request
     * POST /ship-navigation-license-request
     */
    suspend fun createIssueRequest(shipInfoId: Long): Result<NavigationRequestResDto> {
        return try {
            val requestJson = """{"shipInfo":$shipInfoId}"""
            println("📤 Creating issue request with body: $requestJson")

            when (val response = repo.onPostAuthJson("ship-navigation-license-request", requestJson)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Create issue request response: $responseJson")

                    val data = responseJson.jsonObject.getValue("data").jsonObject
                    val result = json.decodeFromJsonElement(NavigationRequestResDto.serializer(), data)
                    Result.success(result)
                }
                is RepoServiceState.Error -> {
                    println("❌ Error creating issue request: ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in createIssueRequest: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Add navigation areas in bulk (Issue)
     * POST /ship-navigation-license-request/{requestId}/navigation-areas
     */
    suspend fun addNavigationAreasIssue(
        requestId: Long,
        areaIds: List<Int>
    ): Result<NavigationRequestResDto> {
        return try {
            val requestJson = """{"areaIds":${json.encodeToString(areaIds)}}"""
            println("📤 Adding navigation areas (Issue): $requestJson")

            when (val response = repo.onPostAuthJson("ship-navigation-license-request/$requestId/navigation-areas", requestJson)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    val data = responseJson.jsonObject.getValue("data").jsonObject
                    val result = json.decodeFromJsonElement(NavigationRequestResDto.serializer(), data)
                    Result.success(result)
                }
                is RepoServiceState.Error -> {
                    println("❌ Error adding navigation areas (Issue): ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in addNavigationAreasIssue: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Add crew members in bulk (Issue)
     * POST /ship-navigation-license-request/{requestId}/crew-list
     */
    suspend fun addCrewBulkIssue(
        requestId: Long,
        crewList: List<CrewReqDto>
    ): Result<List<CrewResDto>> {
        return try {
            val requestJson = json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(CrewReqDto.serializer()),
                crewList
            )
            println("📤 Adding crew bulk (Issue): $requestJson")

            when (val response = repo.onPostAuthJson("ship-navigation-license-request/$requestId/crew-list", requestJson)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    val data = responseJson.jsonObject.getValue("data").jsonArray
                    val result = data.map { json.decodeFromJsonElement(CrewResDto.serializer(), it) }
                    Result.success(result)
                }
                is RepoServiceState.Error -> {
                    println("❌ Error adding crew bulk (Issue): ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in addCrewBulkIssue: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Upload crew Excel file (Issue)
     * POST /ship-navigation-license-request/{requestId}/crew/upload
     */
    suspend fun uploadCrewExcelIssue(
        requestId: Long,
        fileParts: List<PartData>
    ): Result<List<CrewResDto>> {
        return try {
            println("📤 Uploading crew Excel (Issue) to /crew/upload")

            when (val response = repo.onPostMultipart("ship-navigation-license-request/$requestId/crew/upload-excel", fileParts)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    val data = responseJson.jsonObject.getValue("data").jsonArray
                    val result = data.map { json.decodeFromJsonElement(CrewResDto.serializer(), it) }
                    Result.success(result)
                }
                is RepoServiceState.Error -> {
                    println("❌ Error uploading crew Excel (Issue): ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in uploadCrewExcelIssue: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get crew members (Issue)
     * GET /ship-navigation-license-request/{requestId}/crew
     */
    suspend fun getCrewIssue(requestId: Long): Result<List<CrewResDto>> {
        return try {
            println("📥 Getting crew list (Issue)")

            when (val response = repo.onGet("ship-navigation-license-request/$requestId/crew")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    val data = responseJson.jsonObject.getValue("data").jsonArray
                    val result = data.map { json.decodeFromJsonElement(CrewResDto.serializer(), it) }
                    Result.success(result)
                }
                is RepoServiceState.Error -> {
                    println("❌ Error getting crew (Issue): ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in getCrewIssue: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update crew member (Issue)
     * PUT /ship-navigation-license-request/{requestId}/crew/{crewId}
     */
    suspend fun updateCrewMemberIssue(
        requestId: Long,
        crewId: Long,
        crew: CrewReqDto
    ): Result<CrewResDto> {
        return try {
            val requestJson = json.encodeToString(CrewReqDto.serializer(), crew)
            println("📤 Updating crew member (Issue): $requestJson")

            when (val response = repo.onPutAuth("ship-navigation-license-request/$requestId/crew/$crewId", requestJson)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    val data = responseJson.jsonObject.getValue("data").jsonObject
                    val result = json.decodeFromJsonElement(CrewResDto.serializer(), data)
                    Result.success(result)
                }
                is RepoServiceState.Error -> {
                    println("❌ Error updating crew member (Issue): ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in updateCrewMemberIssue: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Delete crew member (Issue)
     * DELETE /ship-navigation-license-request/{requestId}/crew/{crewId}
     * Note: AppRepository doesn't have delete method, so we'll use onPutAuth or handle differently
     */
    suspend fun deleteCrewMemberIssue(requestId: Long, crewId: Long): Result<Unit> {
        return try {
            println("🗑️ Deleting crew member (Issue) - requestId=$requestId, crewId=$crewId")
            // TODO: Implement when backend provides DELETE support or use alternative method
            Result.failure(Exception("Delete not implemented yet - waiting for backend"))
        } catch (e: Exception) {
            println("❌ Exception in deleteCrewMemberIssue: ${e.message}")
            Result.failure(e)
        }
    }

    // ========================================
    // RENEW NAVIGATION LICENSE
    // ========================================

    /**
     * Create a new navigation license renewal request
     * POST /navigation-license-renewal-request
     */
    suspend fun createRenewalRequest(
        shipInfoId: Long,
        lastNavLicId: Long
    ): Result<NavigationRequestResDto> {
        return try {
            val requestJson = """{"shipInfo":$shipInfoId,"lastNavLicId":$lastNavLicId}"""
            println("📤 Creating renewal request with body: $requestJson")

            when (val response = repo.onPostAuthJson("navigation-license-renewal-request", requestJson)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Create renewal request response: $responseJson")

                    val data = responseJson.jsonObject.getValue("data").jsonObject
                    val result = json.decodeFromJsonElement(NavigationRequestResDto.serializer(), data)
                    Result.success(result)
                }
                is RepoServiceState.Error -> {
                    println("❌ Error creating renewal request: ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in createRenewalRequest: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Create a new navigation license renewal request (simple body)
     * POST /navigation-license-renewal-request
     * Body: {"shipInfo": <id>}
     */
    suspend fun createRenewalRequestSimple(
        shipInfoId: Long
    ): Result<NavigationRequestResDto> {
        return try {
            val requestJson = """{"shipInfo":$shipInfoId}"""
            println("📤 Creating renewal request (simple body) with body: $requestJson")

            when (val response = repo.onPostAuthJson("navigation-license-renewal-request", requestJson)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Create renewal request (simple) response: $responseJson")

                    val data = responseJson.jsonObject.getValue("data").jsonObject
                    val result = json.decodeFromJsonElement(NavigationRequestResDto.serializer(), data)
                    Result.success(result)
                }
                is RepoServiceState.Error -> {
                    println("❌ Error creating renewal request (simple): ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in createRenewalRequestSimple: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get existing navigation areas (Renew)
     * GET /navigation-license-renewal-request/{requestId}/navigation-areas
     */
    suspend fun getNavigationAreasRenew(requestId: Long): Result<List<NavigationAreaResDto>> {
        return try {
            println("📥 Getting navigation areas (Renew)")

            when (val response = repo.onGet("navigation-license-renewal-request/$requestId/navigation-areas")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    val data = responseJson.jsonObject.getValue("data").jsonArray
                    // The renewal API returns items like {"id":2,"nameAr":"المنطقة 2","nameEn":"Area 2"}
                    // Map these into NavigationAreaResDto so existing code can consume areaNameAr/areaNameEn
                    val result = data.map { elem ->
                        try {
                            val obj = elem.jsonObject
                            val idLong = obj["id"]?.jsonPrimitive?.intOrNull?.toLong() ?: 0L
                            val nameAr = obj["nameAr"]?.jsonPrimitive?.contentOrNull ?: ""
                            val nameEn = obj["nameEn"]?.jsonPrimitive?.contentOrNull ?: ""
                            // areaId: use the integer id if present otherwise 0
                            val areaId = obj["id"]?.jsonPrimitive?.intOrNull ?: 0
                            NavigationAreaResDto(
                                id = idLong,
                                areaId = areaId,
                                areaNameAr = nameAr,
                                areaNameEn = nameEn,
                                attachmentFile = null,
                                shipNavigationRequestId = requestId
                            )
                        } catch (e: Exception) {
                            println("⚠️ Failed to map navigation area element: ${e.message}")
                            // Return a placeholder with minimal data
                            NavigationAreaResDto(
                                id = 0L,
                                areaId = 0,
                                areaNameAr = "",
                                areaNameEn = "",
                                attachmentFile = null,
                                shipNavigationRequestId = requestId
                            )
                        }
                    }
                    Result.success(result)
                }
                is RepoServiceState.Error -> {
                    println("❌ Error getting navigation areas (Renew): ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in getNavigationAreasRenew: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Add navigation areas in bulk (Renew)
     * POST /navigation-license-renewal-request/{requestId}/navigation-areas
     */
    suspend fun addNavigationAreasRenew(
        requestId: Long,
        areaIds: List<Int>
    ): Result<NavigationRequestResDto> {
        return try {
            val requestJson = """{"areaIds":${json.encodeToString(areaIds)}}"""
            println("📤 Adding navigation areas (Renew): $requestJson")

            when (val response = repo.onPostAuthJson("navigation-license-renewal-request/$requestId/navigation-areas", requestJson)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    val data = responseJson.jsonObject.getValue("data").jsonObject
                    val result = json.decodeFromJsonElement(NavigationRequestResDto.serializer(), data)
                    Result.success(result)
                }
                is RepoServiceState.Error -> {
                    println("❌ Error adding navigation areas (Renew): ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in addNavigationAreasRenew: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update navigation areas (Renew)
     * PUT /navigation-license-renewal-request/{requestId}/navigation-areas
     */
    suspend fun updateNavigationAreasRenew(
        requestId: Long,
        areaIds: List<Int>
    ): Result<NavigationRequestResDto> {
        return try {
            val requestJson = """{"areaIds":${json.encodeToString(areaIds)}}"""
            println("📤 Updating navigation areas (Renew): $requestJson")

            when (val response = repo.onPutAuth("navigation-license-renewal-request/$requestId/navigation-areas", requestJson)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    val data = responseJson.jsonObject.getValue("data").jsonObject
                    val result = json.decodeFromJsonElement(NavigationRequestResDto.serializer(), data)
                    Result.success(result)
                }
                is RepoServiceState.Error -> {
                    println("❌ Error updating navigation areas (Renew): ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in updateNavigationAreasRenew: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get existing crew members (Renew)
     * GET /navigation-license-renewal-request/{lastNavLicId}/crew
     */
    suspend fun getCrewRenew(lastNavLicId: Long): Result<List<CrewResDto>> {
        return try {
            println("📥 Getting crew list (Renew) for lastNavLicId=$lastNavLicId")

            when (val response = repo.onGet("navigation-license-renewal-request/$lastNavLicId/crew")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    val data = responseJson.jsonObject.getValue("data").jsonArray
                    val result = data.map { json.decodeFromJsonElement(CrewResDto.serializer(), it) }

                    println("✅ Successfully loaded ${result.size} crew members")
                    result.forEachIndexed { index, crew ->
                        println("   [$index] ${crew.nameEn} - Job: ${crew.jobTitle.nameAr}, Nationality: ${crew.nationality?.nameAr ?: "N/A"} (${crew.nationality?.id ?: "N/A"})")
                    }

                    Result.success(result)
                }
                is RepoServiceState.Error -> {
                    println("❌ Error getting crew (Renew): ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in getCrewRenew: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Add crew members in bulk (Renew)
     * POST /navigation-license-renewal-request/{requestId}/crew-list
     */
    suspend fun addCrewBulkRenew(
        requestId: Long,
        crewList: List<CrewReqDto>
    ): Result<List<CrewResDto>> {
        return try {
            val requestJson = json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(CrewReqDto.serializer()),
                crewList
            )
            println("📤 Adding crew bulk (Renew): $requestJson")

            when (val response = repo.onPostAuthJson("navigation-license-renewal-request/$requestId/crew-list", requestJson)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    val data = responseJson.jsonObject.getValue("data").jsonArray
                    val result = data.map { json.decodeFromJsonElement(CrewResDto.serializer(), it) }
                    Result.success(result)
                }
                is RepoServiceState.Error -> {
                    println("❌ Error adding crew bulk (Renew): ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in addCrewBulkRenew: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update crew member (Renew)
     * PUT /navigation-license-renewal-request/{requestId}/crew/{crewId}
     */
    suspend fun updateCrewMemberRenew(
        requestId: Long,
        crewId: Long,
        crew: CrewReqDto
    ): Result<CrewResDto> {
        return try {
            val requestJson = json.encodeToString(CrewReqDto.serializer(), crew)
            println("📤 Updating crew member (Renew): $requestJson")

            when (val response = repo.onPutAuth("navigation-license-renewal-request/$requestId/crew/$crewId", requestJson)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    val data = responseJson.jsonObject.getValue("data").jsonObject
                    val result = json.decodeFromJsonElement(CrewResDto.serializer(), data)
                    Result.success(result)
                }
                is RepoServiceState.Error -> {
                    println("❌ Error updating crew member (Renew): ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in updateCrewMemberRenew: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Add a single crew member (Renew)
     * POST /navigation-license-renewal-request/{requestId}/crew
     */
    suspend fun addCrewMemberRenew(requestId: Long, crew: CrewReqDto): Result<CrewResDto> {
        return try {
            val requestJson = json.encodeToString(CrewReqDto.serializer(), crew)
            println("📤 Adding single crew member (Renew): requestId=$requestId, body=$requestJson")

            when (val response = repo.onPostAuthJson("navigation-license-renewal-request/$requestId/crew", requestJson)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    val data = responseJson.jsonObject.getValue("data").jsonObject
                    val result = json.decodeFromJsonElement(CrewResDto.serializer(), data)
                    println("✅ Crew member added successfully with id=${result.id}")
                    Result.success(result)
                }
                is RepoServiceState.Error -> {
                    println("❌ Error adding single crew member (Renew): ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in addCrewMemberRenew: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Delete crew member (Renew)
     * DELETE /navigation-license-renewal-request/{requestId}/crew/{crewId}
     */
    suspend fun deleteCrewMemberRenew(requestId: Long, crewId: Long): Result<Unit> {
        return try {
            println("🗑️ Deleting crew member (Renew) - requestId=$requestId, crewId=$crewId")

            when (val response = repo.onDeleteAuth("navigation-license-renewal-request/$requestId/crew/$crewId")) {
                is RepoServiceState.Success -> {
                    println("✅ Crew member deleted successfully")
                    Result.success(Unit)
                }
                is RepoServiceState.Error -> {
                    println("❌ Error deleting crew member (Renew): ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in deleteCrewMemberRenew: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Upload crew Excel file (Renew)
     * POST /navigation-license-renewal-request/{requestId}/crew/upload
     */
    suspend fun uploadCrewExcelRenew(
        requestId: Long,
        fileParts: List<PartData>
    ): Result<List<CrewResDto>> {
        return try {
            println("📤 Uploading crew Excel (Renew) to /crew/upload")

            when (val response = repo.onPostMultipart("navigation-license-renewal-request/$requestId/crew/upload-excel", fileParts)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    val data = responseJson.jsonObject.getValue("data").jsonArray
                    val result = data.map { json.decodeFromJsonElement(CrewResDto.serializer(), it) }
                    Result.success(result)
                }
                is RepoServiceState.Error -> {
                    println("❌ Error uploading crew Excel (Renew): ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in uploadCrewExcelRenew: ${e.message}")
            Result.failure(e)
        }
    }

    // ========================================
    // CHANGE CAPTAIN (requestTypeId = 10)
    // ========================================

    /**
     * Create a change captain request (CDD §4.1.6)
     * POST /change-captain/{shipInfoId}/add-request
     * Body: array of LicShipInfoCrewListReqDto
     * Response: ResponseDto<ShipNavigationLicenseRequestResDto> → id, requestSerial
     */
    /**
     * Get existing captains for a ship (CDD §4.1.3)
     * GET /change-captain/{shipInfoId}/captains
     */
    suspend fun getExistingCaptains(shipInfoId: Long): Result<List<CrewResDto>> {
        return try {
            println("📥 Getting existing captains for shipInfoId=$shipInfoId")
            when (val response = repo.onGet("change-captain/$shipInfoId/captains")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Existing captains response: $responseJson")
                    val dataArray = responseJson.jsonObject["data"]?.jsonArray ?: run {
                        println("⚠️ No 'data' array in captains response")
                        return Result.success(emptyList())
                    }
                    val captains = dataArray.mapNotNull { element ->
                        try {
                            val obj = element.jsonObject
                            val id = obj["id"]?.jsonPrimitive?.content?.toLongOrNull() ?: return@mapNotNull null
                            val nameAr = obj["nameAr"]?.jsonPrimitive?.contentOrNull ?: ""
                            val nameEn = obj["nameEn"]?.jsonPrimitive?.contentOrNull ?: ""
                            val jobTitleObj = obj["jobTitle"]?.jsonObject
                            val jobTitleId = jobTitleObj?.get("id")?.jsonPrimitive?.intOrNull ?: 0
                            val jobTitleNameAr = jobTitleObj?.get("nameAr")?.jsonPrimitive?.contentOrNull ?: ""
                            val jobTitleNameEn = jobTitleObj?.get("nameEn")?.jsonPrimitive?.contentOrNull ?: ""
                            val civilNo = obj["civilNo"]?.jsonPrimitive?.contentOrNull
                            val seamenBookNo = obj["seamenBookNo"]?.jsonPrimitive?.contentOrNull ?: ""
                            val nationalityObj = obj["nationality"]?.jsonObject
                            val nationalityId = nationalityObj?.get("id")?.jsonPrimitive?.contentOrNull
                            val nationality = nationalityId?.let {
                                com.informatique.mtcit.data.dto.CountryResDto(
                                    id = it,
                                    nameAr = nationalityObj?.get("nameAr")?.jsonPrimitive?.contentOrNull ?: "",
                                    nameEn = nationalityObj?.get("nameEn")?.jsonPrimitive?.contentOrNull ?: ""
                                )
                            }
                            CrewResDto(
                                id = id,
                                nameAr = nameAr,
                                nameEn = nameEn,
                                jobTitle = com.informatique.mtcit.data.dto.JobTitleResDto(
                                    id = jobTitleId,
                                    nameAr = jobTitleNameAr,
                                    nameEn = jobTitleNameEn
                                ),
                                civilNo = civilNo,
                                seamenBookNo = seamenBookNo,
                                nationality = nationality,
                                shipNavigationRequestId = 0L,
                                shipInfoId = shipInfoId
                            )
                        } catch (e: Exception) {
                            println("⚠️ Failed to parse captain entry: ${e.message}")
                            null
                        }
                    }
                    println("✅ Parsed ${captains.size} existing captains")
                    Result.success(captains)
                }
                is RepoServiceState.Error -> {
                    val msg = ErrorMessageExtractor.extract(response.error)
                    println("❌ Get captains error: $msg")
                    Result.failure(ApiException(response.code, msg))
                }
            }
        } catch (e: ApiException) { throw e }
        catch (e: Exception) {
            println("❌ Exception in getExistingCaptains: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun createChangeCaptainRequest(
        shipInfoId: Long,
        crewList: List<Map<String, String>>
    ): Result<NavigationRequestResDto> {
        return try {
            println("📤 Creating change captain request for shipInfoId=$shipInfoId, crew=${crewList.size}")

            // Build crew JSON array — do NOT include id/apiId (CDD §4.1.6: no id in request body)
            val crewJson = crewList.joinToString(prefix = "[", postfix = "]") { crew ->
                val jobTitle = crew["jobTitle"]?.toIntOrNull() ?: 0
                val nationalityId = crew["nationality"] ?: crew["nationalityId"] ?: ""
                """{"nameAr":"${crew["nameAr"] ?: ""}","nameEn":"${crew["nameEn"] ?: ""}","jobTitle":$jobTitle,"civilNo":"${crew["civilNo"] ?: ""}","seamenBookNo":"${crew["seamenBookNo"] ?: ""}","nationality":{"id":"$nationalityId"}}"""
            }

            println("📤 Request body: $crewJson")

            when (val response = repo.onPostAuthJson("change-captain/$shipInfoId/add-request", crewJson)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Change captain request response: $responseJson")

                    // Response is ResponseDto<ShipNavigationLicenseRequestResDto>
                    val dataObj = responseJson.jsonObject["data"]?.jsonObject
                    val id = dataObj?.get("id")?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                    val requestSerial = dataObj?.get("requestSerial")?.jsonPrimitive?.intOrNull

                    println("✅ Created change captain request id=$id, serial=$requestSerial")
                    Result.success(NavigationRequestResDto(id = id, requestSerial = requestSerial))
                }
                is RepoServiceState.Error -> {
                    val msg = ErrorMessageExtractor.extract(response.error)
                    println("❌ Change captain request error: $msg")
                    Result.failure(ApiException(response.code, msg))
                }
            }
        } catch (e: ApiException) { throw e }
        catch (e: Exception) {
            println("❌ Exception in createChangeCaptainRequest: ${e.message}")
            Result.failure(e)
        }
    }
}

// ========================================
// REQUEST DTOs
// ========================================

/**
 * Simple DTO for navigation area lookup data returned in responses
 * This matches the API response structure for licMdNavAreasResDto
 */
@Serializable
data class NavigationAreaLookupDto(
    val id: Int,
    val nameAr: String,
    val nameEn: String
)

@Serializable
data class NavigationRequestResDto(
    val id: Long,
    val requestSerial: Int? = null,
    val requestYear: Int? = null,
    val licMdNavAreasResDto: List<NavigationAreaLookupDto>? = null,  // ✅ Use simple lookup DTO
    val crewList: List<CrewResDto>? = null,
    val lastNavLicId: Long? = null
)
