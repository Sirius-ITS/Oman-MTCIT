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
import kotlinx.serialization.json.int
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
     * POST /api/v1/ship-navigation-license-request
     */
    suspend fun createIssueRequest(shipInfoId: Long): Result<NavigationRequestResDto> {
        return try {
            val requestJson = """{"shipInfo":$shipInfoId}"""
            println("📤 Creating issue request with body: $requestJson")

            when (val response = repo.onPostAuthJson("api/v1/ship-navigation-license-request", requestJson)) {
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
     * POST /api/v1/ship-navigation-license-request/{requestId}/navigation-areas
     */
    suspend fun addNavigationAreasIssue(
        requestId: Long,
        areaIds: List<Int>
    ): Result<NavigationRequestResDto> {
        return try {
            val requestJson = """{"areaIds":${json.encodeToString(areaIds)}}"""
            println("📤 Adding navigation areas (Issue): $requestJson")

            when (val response = repo.onPostAuthJson("api/v1/ship-navigation-license-request/$requestId/navigation-areas", requestJson)) {
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
     * POST /api/v1/ship-navigation-license-request/{requestId}/crew-list
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

            when (val response = repo.onPostAuthJson("api/v1/ship-navigation-license-request/$requestId/crew-list", requestJson)) {
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
     * POST /api/v1/ship-navigation-license-request/{requestId}/crew/upload-excel
     */
    suspend fun uploadCrewExcelIssue(
        requestId: Long,
        fileParts: List<PartData>
    ): Result<List<CrewResDto>> {
        return try {
            println("📤 Uploading crew Excel (Issue)")

            when (val response = repo.onPostMultipart("api/v1/ship-navigation-license-request/$requestId/crew/upload-excel", fileParts)) {
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
     * GET /api/v1/ship-navigation-license-request/{requestId}/crew
     */
    suspend fun getCrewIssue(requestId: Long): Result<List<CrewResDto>> {
        return try {
            println("📥 Getting crew list (Issue)")

            when (val response = repo.onGet("api/v1/ship-navigation-license-request/$requestId/crew")) {
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
     * PUT /api/v1/ship-navigation-license-request/{requestId}/crew/{crewId}
     */
    suspend fun updateCrewMemberIssue(
        requestId: Long,
        crewId: Long,
        crew: CrewReqDto
    ): Result<CrewResDto> {
        return try {
            val requestJson = json.encodeToString(CrewReqDto.serializer(), crew)
            println("📤 Updating crew member (Issue): $requestJson")

            when (val response = repo.onPutAuth("api/v1/ship-navigation-license-request/$requestId/crew/$crewId", requestJson)) {
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
     * DELETE /api/v1/ship-navigation-license-request/{requestId}/crew/{crewId}
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
     * POST /api/v1/navigation-license-renewal-request
     */
    suspend fun createRenewalRequest(
        shipInfoId: Long,
        lastNavLicId: Long
    ): Result<NavigationRequestResDto> {
        return try {
            val requestJson = """{"shipInfo":$shipInfoId,"lastNavLicId":$lastNavLicId}"""
            println("📤 Creating renewal request with body: $requestJson")

            when (val response = repo.onPostAuthJson("api/v1/navigation-license-renewal-request", requestJson)) {
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
     * Get existing navigation areas (Renew)
     * GET /api/v1/navigation-license-renewal-request/{requestId}/navigation-areas
     */
    suspend fun getNavigationAreasRenew(requestId: Long): Result<List<NavigationAreaResDto>> {
        return try {
            println("📥 Getting navigation areas (Renew)")

            when (val response = repo.onGet("api/v1/navigation-license-renewal-request/$requestId/navigation-areas")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    val data = responseJson.jsonObject.getValue("data").jsonArray
                    val result = data.map { json.decodeFromJsonElement(NavigationAreaResDto.serializer(), it) }
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
     * POST /api/v1/navigation-license-renewal-request/{requestId}/navigation-areas
     */
    suspend fun addNavigationAreasRenew(
        requestId: Long,
        areaIds: List<Int>
    ): Result<NavigationRequestResDto> {
        return try {
            val requestJson = """{"areaIds":${json.encodeToString(areaIds)}}"""
            println("📤 Adding navigation areas (Renew): $requestJson")

            when (val response = repo.onPostAuthJson("api/v1/navigation-license-renewal-request/$requestId/navigation-areas", requestJson)) {
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
     * PUT /api/v1/navigation-license-renewal-request/{requestId}/navigation-areas
     */
    suspend fun updateNavigationAreasRenew(
        requestId: Long,
        areaIds: List<Int>
    ): Result<NavigationRequestResDto> {
        return try {
            val requestJson = """{"areaIds":${json.encodeToString(areaIds)}}"""
            println("📤 Updating navigation areas (Renew): $requestJson")

            when (val response = repo.onPutAuth("api/v1/navigation-license-renewal-request/$requestId/navigation-areas", requestJson)) {
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
     * GET /api/v1/navigation-license-renewal-request/{lastNavLicId}/crew
     */
    suspend fun getCrewRenew(lastNavLicId: Long): Result<List<CrewResDto>> {
        return try {
            println("📥 Getting crew list (Renew)")

            when (val response = repo.onGet("api/v1/navigation-license-renewal-request/$lastNavLicId/crew")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    val data = responseJson.jsonObject.getValue("data").jsonArray
                    val result = data.map { json.decodeFromJsonElement(CrewResDto.serializer(), it) }
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
     * POST /api/v1/navigation-license-renewal-request/{requestId}/crew-list
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

            when (val response = repo.onPostAuthJson("api/v1/navigation-license-renewal-request/$requestId/crew-list", requestJson)) {
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
     * PUT /api/v1/navigation-license-renewal-request/{requestId}/crew/{crewId}
     */
    suspend fun updateCrewMemberRenew(
        requestId: Long,
        crewId: Long,
        crew: CrewReqDto
    ): Result<CrewResDto> {
        return try {
            val requestJson = json.encodeToString(CrewReqDto.serializer(), crew)
            println("📤 Updating crew member (Renew): $requestJson")

            when (val response = repo.onPutAuth("api/v1/navigation-license-renewal-request/$requestId/crew/$crewId", requestJson)) {
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
     * Delete crew member (Renew)
     * DELETE /api/v1/navigation-license-renewal-request/{requestId}/crew/{crewId}
     */
    suspend fun deleteCrewMemberRenew(requestId: Long, crewId: Long): Result<Unit> {
        return try {
            println("🗑️ Deleting crew member (Renew) - requestId=$requestId, crewId=$crewId")
            // TODO: Implement when backend provides DELETE support
            Result.failure(Exception("Delete not implemented yet - waiting for backend"))
        } catch (e: Exception) {
            println("❌ Exception in deleteCrewMemberRenew: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Upload crew Excel file (Renew)
     * POST /api/v1/navigation-license-renewal-request/{requestId}/crew/upload-excel
     */
    suspend fun uploadCrewExcelRenew(
        requestId: Long,
        fileParts: List<PartData>
    ): Result<List<CrewResDto>> {
        return try {
            println("📤 Uploading crew Excel (Renew)")

            when (val response = repo.onPostMultipart("api/v1/navigation-license-renewal-request/$requestId/crew/upload-excel", fileParts)) {
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
}

// ========================================
// REQUEST DTOs
// ========================================

@Serializable
data class NavigationRequestResDto(
    val id: Long,
    val requestSerial: Int? = null,
    val requestYear: Int? = null,
    val licMdNavAreasResDto: List<NavigationAreaResDto>? = null,
    val crewList: List<CrewResDto>? = null,
    val lastNavLicId: Long? = null
)
