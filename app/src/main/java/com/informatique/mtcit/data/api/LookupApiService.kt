package com.informatique.mtcit.data.api

import com.informatique.mtcit.data.model.*
import com.informatique.mtcit.di.module.AppRepository
import com.informatique.mtcit.di.module.RepoServiceState
import com.informatique.mtcit.ui.components.DefaultBusinessIcon
import com.informatique.mtcit.ui.components.PersonType
import com.informatique.mtcit.ui.components.SelectableItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lookup API Service for fetching dropdown options from real APIs
 */
@Singleton
class LookupApiService @Inject constructor(
    private val repo: AppRepository,
    private val json: Json
) {

    /**
     * Get list of ship types
     * API: api/v1/coremdshiptype
     */
    suspend fun getShipTypes(): Result<LookupResponse<ShipType>> {
        return try {
            when (val response = repo.onGet("api/v1/coremdshiptype")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean) {

                            val paginatedData = responseJson.jsonObject.getValue("data").jsonObject
                            val content = paginatedData.getValue("content").jsonArray

                            val shipTypes: List<ShipType> = json.decodeFromJsonElement(content)

                            Result.success(LookupResponse(true, shipTypes))
                        } else {
                            Result.failure(Exception("Failed to fetch ship types"))
                        }
                    } else {
                        Result.failure(Exception("Empty ship types response"))
                    }
                }
                is RepoServiceState.Error -> {
                    Result.failure(Exception("Failed to get ship types: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to get ship types: ${e.message}"))
        }
    }

    /**
     * Get ship types by category ID
     * API: api/v1/coremdshiptype/category/{categoryId}
     */
    suspend fun getShipTypesByCategory(categoryId: Int): Result<LookupResponse<ShipType>> {
        return try {
            when (val response = repo.onGet("api/v1/coremdshiptype/category/$categoryId")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean) {

                            val paginatedData = responseJson.jsonObject.getValue("data").jsonObject
                            val content = paginatedData.getValue("content").jsonArray

                            val shipTypes: List<ShipType> = json.decodeFromJsonElement(content)

                            Result.success(LookupResponse(true, shipTypes))
                        } else {
                            Result.failure(Exception("Failed to fetch ship types by category"))
                        }
                    } else {
                        Result.failure(Exception("Empty ship types response"))
                    }
                }
                is RepoServiceState.Error -> {
                    Result.failure(Exception("Failed to get ship types by category: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to get ship types by category: ${e.message}"))
        }
    }

    /**
     * Get list of ship categories
     * API: api/v1/coremdshipcategory
     */
    suspend fun getShipCategories(): Result<LookupResponse<ShipCategory>> {
        return try {
            when (val response = repo.onGet("api/v1/coremdshipcategory")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean) {

                            val paginatedData = responseJson.jsonObject.getValue("data").jsonObject
                            val content = paginatedData.getValue("content").jsonArray

                            val categories: List<ShipCategory> = json.decodeFromJsonElement(content)

                            Result.success(LookupResponse(true, categories))
                        } else {
                            Result.failure(Exception("Failed to fetch ship categories"))
                        }
                    } else {
                        Result.failure(Exception("Empty ship categories response"))
                    }
                }
                is RepoServiceState.Error -> {
                    Result.failure(Exception("Failed to get ship categories: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to get ship categories: ${e.message}"))
        }
    }

    /**
     * Get list of engine statuses
     * API: api/v1/coremdshipenginestatus
     */
    suspend fun getEngineStatuses(): Result<LookupResponse<EngineStatus>> {
        return try {
            when (val response = repo.onGet("api/v1/coremdshipenginestatus")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean) {

                            val paginatedData = responseJson.jsonObject.getValue("data").jsonObject
                            val content = paginatedData.getValue("content").jsonArray

                            val statuses: List<EngineStatus> = json.decodeFromJsonElement(content)

                            Result.success(LookupResponse(true, statuses))
                        } else {
                            Result.failure(Exception("Failed to fetch engine statuses"))
                        }
                    } else {
                        Result.failure(Exception("Empty engine statuses response"))
                    }
                }
                is RepoServiceState.Error -> {
                    Result.failure(Exception("Failed to get engine statuses: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to get engine statuses: ${e.message}"))
        }
    }

    /**
     * Get list of proof types
     * API: api/v1/coremdprooftype
     */
    suspend fun getProofTypes(): Result<LookupResponse<ProofType>> {
        return try {
            when (val response = repo.onGet("api/v1/coremdprooftype")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean) {

                            val paginatedData = responseJson.jsonObject.getValue("data").jsonObject
                            val content = paginatedData.getValue("content").jsonArray

                            val proofTypes: List<ProofType> = json.decodeFromJsonElement(content)

                            Result.success(LookupResponse(true, proofTypes))
                        } else {
                            Result.failure(Exception("Failed to fetch proof types"))
                        }
                    } else {
                        Result.failure(Exception("Empty proof types response"))
                    }
                }
                is RepoServiceState.Error -> {
                    Result.failure(Exception("Failed to get proof types: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to get proof types: ${e.message}"))
        }
    }

    /**
     * Get list of ports
     * API: api/v1/coremdportofregistry
     */
    suspend fun getPorts(): Result<LookupResponse<Port>> {
        return try {
            when (val response = repo.onGet("api/v1/coremdportofregistry")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean) {

                            val paginatedData = responseJson.jsonObject.getValue("data").jsonObject
                            val content = paginatedData.getValue("content").jsonArray

                            val ports: List<Port> = json.decodeFromJsonElement(content)

                            Result.success(LookupResponse(true, ports))
                        } else {
                            Result.failure(Exception("Failed to fetch ports"))
                        }
                    } else {
                        Result.failure(Exception("Empty ports response"))
                    }
                }
                is RepoServiceState.Error -> {
                    Result.failure(Exception("Failed to get ports: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to get ports: ${e.message}"))
        }
    }

    /**
     * Get list of marine activities
     * API: api/v1/coremdmarineactivity
     */
    suspend fun getMarineActivities(): Result<LookupResponse<MarineActivity>> {
        return try {
            when (val response = repo.onGet("api/v1/coremdmarineactivity")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean) {

                            val paginatedData = responseJson.jsonObject.getValue("data").jsonObject
                            val content = paginatedData.getValue("content").jsonArray

                            val activities: List<MarineActivity> = json.decodeFromJsonElement(content)

                            Result.success(LookupResponse(true, activities))
                        } else {
                            Result.failure(Exception("Failed to fetch marine activities"))
                        }
                    } else {
                        Result.failure(Exception("Empty marine activities response"))
                    }
                }
                is RepoServiceState.Error -> {
                    Result.failure(Exception("Failed to get marine activities: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to get marine activities: ${e.message}"))
        }
    }

    /**
     * Get list of building materials
     * API: api/v1/coremdbuildmaterial
     */
    suspend fun getBuildMaterials(): Result<LookupResponse<BuildMaterial>> {
        return try {
            when (val response = repo.onGet("api/v1/coremdbuildmaterial")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean) {

                            val paginatedData = responseJson.jsonObject.getValue("data").jsonObject
                            val content = paginatedData.getValue("content").jsonArray

                            val materials: List<BuildMaterial> = json.decodeFromJsonElement(content)

                            Result.success(LookupResponse(true, materials))
                        } else {
                            Result.failure(Exception("Failed to fetch building materials"))
                        }
                    } else {
                        Result.failure(Exception("Empty building materials response"))
                    }
                }
                is RepoServiceState.Error -> {
                    Result.failure(Exception("Failed to get building materials: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to get building materials: ${e.message}"))
        }
    }

    /**
     * Get list of countries
     * API: api/v1/coremdcountry
     */
    suspend fun getCountries(): Result<LookupResponse<Country>> {
        return try {
            when (val response = repo.onGet("api/v1/coremdcountry")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean) {

                            val paginatedData = responseJson.jsonObject.getValue("data").jsonObject
                            val content = paginatedData.getValue("content").jsonArray

                            val countries: List<Country> = json.decodeFromJsonElement(content)

                            Result.success(LookupResponse(true, countries))
                        } else {
                            Result.failure(Exception("Failed to fetch countries"))
                        }
                    } else {
                        Result.failure(Exception("Empty countries response"))
                    }
                }
                is RepoServiceState.Error -> {
                    Result.failure(Exception("Failed to get countries: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to get countries: ${e.message}"))
        }
    }

    /**
     * Get cities by country (keeping mock for now - no API provided)
     */
    suspend fun getCitiesByCountry(countryId: String): Result<LookupResponse<City>> {
        // TODO: Replace with real API when available
        return Result.success(LookupResponse(true, emptyList()))
    }

    /**
     * Get list of commercial registrations (keeping mock for now - no API provided)
     */
    suspend fun getCommercialRegistrations(): Result<LookupResponse<SelectableItem>> {
        // TODO: Replace with real API when available
        val data = listOf(
            SelectableItem(
                id = "CR-2024-001",
                title = "شركة النور للتجارة",
                code = "CR-2024-001",
                description = "شركة تجارية متخصصة في استيراد وتصدير\nالمواد الغذائية"
            ),
            SelectableItem(
                id = "CR-2024-002",
                title = "مؤسسة البحر للملاحة",
                code = "CR-2024-002",
                description = "مؤسسة متخصصة في النقل البحري\nوالخدمات اللوجستية"
            )
        )
        return Result.success(LookupResponse(true, data))
    }

    /**
     * Get list of person types (keeping mock for now - no API provided)
     */
    suspend fun getPersonTypes(): Result<LookupResponse<PersonType>> {
        // TODO: Replace with real API when available
        val data = listOf(
            PersonType(
                id = "PT-2024-001",
                title = "فرد",
                code = "PT-2024-001",
                icon = { DefaultBusinessIcon(false) }
            ),
            PersonType(
                id = "PT-2024-002",
                title = "شركة",
                code = "PT-2024-002"
            )
        )
        return Result.success(LookupResponse(true, data))
    }
}
