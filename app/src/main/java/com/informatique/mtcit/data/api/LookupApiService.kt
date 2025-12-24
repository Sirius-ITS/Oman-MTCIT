package com.informatique.mtcit.data.api

import com.informatique.mtcit.common.ErrorMessageExtractor
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
     * API: coremdshiptype
     */
//    suspend fun getShipTypes(): Result<LookupResponse<ShipType>> {
//        return try {
//            when (val response = repo.onGet("core-md-ship-type/ddl")) {
//                is RepoServiceState.Success -> {
//                    val responseJson = response.response
//                    if (!responseJson.jsonObject.isEmpty()) {
//                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
//                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
//                        ) {
//                            // الـ data دلوقتي مباشرة array مش object
//                            val dataArray = responseJson.jsonObject.getValue("data").jsonArray
//
//                            val shipTypes: List<ShipType> = json.decodeFromJsonElement(dataArray)
//
//                            Result.success(LookupResponse(true, shipTypes))
//                        } else {
//                            Result.failure(Exception("Failed to fetch ship types"))
//                        }
//                    } else {
//                        Result.failure(Exception("Empty ship types response"))
//                    }
//                }
//
//                is RepoServiceState.Error -> {
//                    Result.failure(Exception("Failed to get ship types: ${response.error}"))
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Result.failure(Exception("Failed to get ship types: ${e.message}"))
//        }
//    }
    suspend fun getShipTypes(): Result<LookupResponse<ShipType>> {
        return try {
            when (val response = repo.onGet("core-md-ship-type/ddl")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
                        ) {
                            val dataArray = responseJson.jsonObject.getValue("data").jsonArray
                            val shipTypes: List<ShipType> = json.decodeFromJsonElement(dataArray)
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
     * API: coremdshiptype/category/{categoryId}
     */
//    suspend fun getShipTypesByCategory(categoryId: Int): Result<LookupResponse<ShipType>> {
//        return try {
//            when (val response = repo.onGet("core-md-ship-type/category/$categoryId")) {
//                is RepoServiceState.Success -> {
//                    val responseJson = response.response
//                    if (!responseJson.jsonObject.isEmpty()) {
//                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
//                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
//                        ) {
//
//                            val paginatedData = responseJson.jsonObject.getValue("data").jsonObject
//                            val content = paginatedData.getValue("content").jsonArray
//
//                            val shipTypes: List<ShipType> = json.decodeFromJsonElement(content)
//
//                            Result.success(LookupResponse(true, shipTypes))
//                        } else {
//                            Result.failure(Exception("Failed to fetch ship types by category"))
//                        }
//                    } else {
//                        Result.failure(Exception("Empty ship types response"))
//                    }
//                }
//
//                is RepoServiceState.Error -> {
//                    Result.failure(Exception("Failed to get ship types by category: ${response.error}"))
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Result.failure(Exception("Failed to get ship types by category: ${e.message}"))
//        }
//    }
    suspend fun getShipTypesByCategory(categoryId: Int): Result<LookupResponse<ShipType>> {
        return try {
            when (val response = repo.onGet("core-md-ship-type/category/$categoryId")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
                        ) {
                            val dataArray = responseJson.jsonObject.getValue("data").jsonArray
                            val shipTypes: List<ShipType> = json.decodeFromJsonElement(dataArray)
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
     * API: coremdshipcategory
     */
//    suspend fun getShipCategories(): Result<LookupResponse<ShipCategory>> {
//        return try {
//            when (val response = repo.onGet("core-md-ship-category/ddl")) {
//                is RepoServiceState.Success -> {
//                    val responseJson = response.response
//                    if (!responseJson.jsonObject.isEmpty()) {
//                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
//                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
//                        ) {
//
//                            val paginatedData = responseJson.jsonObject.getValue("data").jsonObject
//                            val content = paginatedData.getValue("content").jsonArray
//
//                            val categories: List<ShipCategory> = json.decodeFromJsonElement(content)
//
//                            Result.success(LookupResponse(true, categories))
//                        } else {
//                            Result.failure(Exception("Failed to fetch ship categories"))
//                        }
//                    } else {
//                        Result.failure(Exception("Empty ship categories response"))
//                    }
//                }
//
//                is RepoServiceState.Error -> {
//                    Result.failure(Exception("Failed to get ship categories: ${response.error}"))
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Result.failure(Exception("Failed to get ship categories: ${e.message}"))
//        }
//    }
    suspend fun getShipCategories(): Result<LookupResponse<ShipCategory>> {
        return try {
            when (val response = repo.onGet("core-md-ship-category/ddl")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
                        ) {
                            val dataArray = responseJson.jsonObject.getValue("data").jsonArray
                            val categories: List<ShipCategory> = json.decodeFromJsonElement(dataArray)
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
     * Get list of engine types
     * API: coremdshipenginetypes
     */
//    suspend fun getEngineTypes(): Result<LookupResponse<EngineType>> {
//        return try {
//            when (val response = repo.onGet("core-md-ship-engine-type/ddl")) {
//                is RepoServiceState.Success -> {
//                    val responseJson = response.response
//                    if (!responseJson.jsonObject.isEmpty()) {
//                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
//                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
//                        ) {
//
//                            val paginatedData = responseJson.jsonObject.getValue("data").jsonObject
//                            val content = paginatedData.getValue("content").jsonArray
//
//                            val types: List<EngineType> = json.decodeFromJsonElement(content)
//
//                            Result.success(LookupResponse(true, types))
//                        } else {
//                            Result.failure(Exception("Failed to fetch engine types"))
//                        }
//                    } else {
//                        Result.failure(Exception("Empty engine types response"))
//                    }
//                }
//
//                is RepoServiceState.Error -> {
//                    Result.failure(Exception("Failed to get engine types: ${response.error}"))
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Result.failure(Exception("Failed to get engine types: ${e.message}"))
//        }
//    }
    suspend fun getEngineTypes(): Result<LookupResponse<EngineType>> {
        return try {
            when (val response = repo.onGet("core-md-ship-engine-type/ddl")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
                        ) {
                            // الـ data دلوقتي مباشرة array مش object
                            val dataArray = responseJson.jsonObject.getValue("data").jsonArray

                            val types: List<EngineType> = json.decodeFromJsonElement(dataArray)

                            Result.success(LookupResponse(true, types))
                        } else {
                            Result.failure(Exception("Failed to fetch engine types"))
                        }
                    } else {
                        Result.failure(Exception("Empty engine types response"))
                    }
                }

                is RepoServiceState.Error -> {
                    Result.failure(Exception("Failed to get engine types: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to get engine types: ${e.message}"))
        }
    }

    /**
     * Get list of engine statuses
     * API: coremdshipenginestatus
     */
//    suspend fun getEngineStatuses(): Result<LookupResponse<EngineStatus>> {
//        return try {
//            when (val response = repo.onGet("core-md-ship-engine-status/ddl")) {
//                is RepoServiceState.Success -> {
//                    val responseJson = response.response
//                    if (!responseJson.jsonObject.isEmpty()) {
//                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
//                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
//                        ) {
//
//                            val paginatedData = responseJson.jsonObject.getValue("data").jsonObject
//                            val content = paginatedData.getValue("content").jsonArray
//
//                            val statuses: List<EngineStatus> = json.decodeFromJsonElement(content)
//
//                            Result.success(LookupResponse(true, statuses))
//                        } else {
//                            Result.failure(Exception("Failed to fetch engine statuses"))
//                        }
//                    } else {
//                        Result.failure(Exception("Empty engine statuses response"))
//                    }
//                }
//
//                is RepoServiceState.Error -> {
//                    Result.failure(Exception("Failed to get engine statuses: ${response.error}"))
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Result.failure(Exception("Failed to get engine statuses: ${e.message}"))
//        }
//    }
    suspend fun getEngineStatuses(): Result<LookupResponse<EngineStatus>> {
        return try {
            when (val response = repo.onGet("core-md-ship-engine-status/ddl")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
                        ) {
                            val dataArray = responseJson.jsonObject.getValue("data").jsonArray
                            val statuses: List<EngineStatus> = json.decodeFromJsonElement(dataArray)
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
     * Get list of engine fuel type
     * API: coremdshipenginefueltype
     */
//    suspend fun getEngineFuelTypes(): Result<LookupResponse<FuelType>> {
//        return try {
//            when (val response = repo.onGet("core-md-ship-engine-fuel-type/ddl")) {
//                is RepoServiceState.Success -> {
//                    val responseJson = response.response
//                    if (!responseJson.jsonObject.isEmpty()) {
//                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
//                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
//                        ) {
//
//                            val paginatedData = responseJson.jsonObject.getValue("data").jsonObject
//                            val content = paginatedData.getValue("content").jsonArray
//
//                            val fuelTypes: List<FuelType> = json.decodeFromJsonElement(content)
//
//                            Result.success(LookupResponse(true, fuelTypes))
//                        } else {
//                            Result.failure(Exception("Failed to fetch fuel types"))
//                        }
//                    } else {
//                        Result.failure(Exception("Empty fuel types response"))
//                    }
//                }
//
//                is RepoServiceState.Error -> {
//                    Result.failure(Exception("Failed to get fuel types: ${response.error}"))
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Result.failure(Exception("Failed to get fuel types: ${e.message}"))
//        }
//    }
    suspend fun getEngineFuelTypes(): Result<LookupResponse<FuelType>> {
        return try {
            when (val response = repo.onGet("core-md-ship-engine-fuel-type/ddl")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
                        ) {
                            val dataArray = responseJson.jsonObject.getValue("data").jsonArray
                            val fuelTypes: List<FuelType> = json.decodeFromJsonElement(dataArray)
                            Result.success(LookupResponse(true, fuelTypes))
                        } else {
                            Result.failure(Exception("Failed to fetch fuel types"))
                        }
                    } else {
                        Result.failure(Exception("Empty fuel types response"))
                    }
                }

                is RepoServiceState.Error -> {
                    Result.failure(Exception("Failed to get fuel types: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to get fuel types: ${e.message}"))
        }
    }

    /**
     * Get list of proof types
     * API: coremdprooftype
     */
//    suspend fun getProofTypes(): Result<LookupResponse<ProofType>> {
//        return try {
//            when (val response = repo.onGet("core-md-proof-type/ddl")) {
//                is RepoServiceState.Success -> {
//                    val responseJson = response.response
//                    if (!responseJson.jsonObject.isEmpty()) {
//                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
//                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
//                        ) {
//
//                            val paginatedData = responseJson.jsonObject.getValue("data").jsonObject
//                            val content = paginatedData.getValue("content").jsonArray
//
//                            val proofTypes: List<ProofType> = json.decodeFromJsonElement(content)
//
//                            Result.success(LookupResponse(true, proofTypes))
//                        } else {
//                            Result.failure(Exception("Failed to fetch proof types"))
//                        }
//                    } else {
//                        Result.failure(Exception("Empty proof types response"))
//                    }
//                }
//
//                is RepoServiceState.Error -> {
//                    Result.failure(Exception("Failed to get proof types: ${response.error}"))
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Result.failure(Exception("Failed to get proof types: ${e.message}"))
//        }
//    }
    suspend fun getProofTypes(): Result<LookupResponse<ProofType>> {
        return try {
            when (val response = repo.onGet("core-md-proof-type/ddl")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
                        ) {
                            val dataArray = responseJson.jsonObject.getValue("data").jsonArray
                            val proofTypes: List<ProofType> = json.decodeFromJsonElement(dataArray)
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
     * API: coremdportofregistry
     */
//    suspend fun getPorts(): Result<LookupResponse<Port>> {
//        return try {
//            when (val response = repo.onGet("core-md-port-of-registry/ddl")) {
//                is RepoServiceState.Success -> {
//                    val responseJson = response.response
//                    if (!responseJson.jsonObject.isEmpty()) {
//                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
//                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
//                        ) {
//
//                            val paginatedData = responseJson.jsonObject.getValue("data").jsonObject
//                            val content = paginatedData.getValue("content").jsonArray
//
//                            val ports: List<Port> = json.decodeFromJsonElement(content)
//
//                            Result.success(LookupResponse(true, ports))
//                        } else {
//                            Result.failure(Exception("Failed to fetch ports"))
//                        }
//                    } else {
//                        Result.failure(Exception("Empty ports response"))
//                    }
//                }
//
//                is RepoServiceState.Error -> {
//                    Result.failure(Exception("Failed to get ports: ${response.error}"))
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Result.failure(Exception("Failed to get ports: ${e.message}"))
//        }
//    }
    suspend fun getPorts(): Result<LookupResponse<Port>> {
        return try {
            when (val response = repo.onGet("core-md-port-of-registry/ddl")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
                        ) {
                            val dataArray = responseJson.jsonObject.getValue("data").jsonArray
                            val ports: List<Port> = json.decodeFromJsonElement(dataArray)
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
     * API: coremdmarineactivity
     */
//    suspend fun getMarineActivities(): Result<LookupResponse<MarineActivity>> {
//        return try {
//            when (val response = repo.onGet("core-md-marine-activity/ddl")) {
//                is RepoServiceState.Success -> {
//                    val responseJson = response.response
//                    if (!responseJson.jsonObject.isEmpty()) {
//                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
//                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
//                        ) {
//
//                            val paginatedData = responseJson.jsonObject.getValue("data").jsonObject
//                            val content = paginatedData.getValue("content").jsonArray
//
//                            val activities: List<MarineActivity> =
//                                json.decodeFromJsonElement(content)
//
//                            Result.success(LookupResponse(true, activities))
//                        } else {
//                            Result.failure(Exception("Failed to fetch marine activities"))
//                        }
//                    } else {
//                        Result.failure(Exception("Empty marine activities response"))
//                    }
//                }
//
//                is RepoServiceState.Error -> {
//                    Result.failure(Exception("Failed to get marine activities: ${response.error}"))
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Result.failure(Exception("Failed to get marine activities: ${e.message}"))
//        }
//    }
    suspend fun getMarineActivities(): Result<LookupResponse<MarineActivity>> {
        return try {
            when (val response = repo.onGet("core-md-marine-activity/ddl")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
                        ) {
                            val dataArray = responseJson.jsonObject.getValue("data").jsonArray
                            val activities: List<MarineActivity> = json.decodeFromJsonElement(dataArray)
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
     * API: coremdbuildmaterial
     */
//    suspend fun getBuildMaterials(): Result<LookupResponse<BuildMaterial>> {
//        return try {
//            when (val response = repo.onGet("core-md-build-material/ddl")) {
//                is RepoServiceState.Success -> {
//                    val responseJson = response.response
//                    if (!responseJson.jsonObject.isEmpty()) {
//                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
//                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
//                        ) {
//
//                            val paginatedData = responseJson.jsonObject.getValue("data").jsonObject
//                            val content = paginatedData.getValue("content").jsonArray
//
//                            val materials: List<BuildMaterial> = json.decodeFromJsonElement(content)
//
//                            Result.success(LookupResponse(true, materials))
//                        } else {
//                            Result.failure(Exception("Failed to fetch building materials"))
//                        }
//                    } else {
//                        Result.failure(Exception("Empty building materials response"))
//                    }
//                }
//
//                is RepoServiceState.Error -> {
//                    Result.failure(Exception("Failed to get building materials: ${response.error}"))
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Result.failure(Exception("Failed to get building materials: ${e.message}"))
//        }
//    }
    suspend fun getBuildMaterials(): Result<LookupResponse<BuildMaterial>> {
        return try {
            when (val response = repo.onGet("core-md-build-material/ddl")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
                        ) {
                            val dataArray = responseJson.jsonObject.getValue("data").jsonArray
                            val materials: List<BuildMaterial> = json.decodeFromJsonElement(dataArray)
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
     * API: coremdcountry
     */
//    suspend fun getCountries(): Result<LookupResponse<Country>> {
//        return try {
//            when (val response = repo.onGet("core-md-country/ddl")) {
//                is RepoServiceState.Success -> {
//                    val responseJson = response.response
//                    if (!responseJson.jsonObject.isEmpty()) {
//                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
//                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
//                        ) {
//
//                            val paginatedData = responseJson.jsonObject.getValue("data").jsonObject
//                            val content = paginatedData.getValue("content").jsonArray
//
//                            val countries: List<Country> = json.decodeFromJsonElement(content)
//
//                            Result.success(LookupResponse(true, countries))
//                        } else {
//                            Result.failure(Exception("Failed to fetch countries"))
//                        }
//                    } else {
//                        Result.failure(Exception("Empty countries response"))
//                    }
//                }
//
//                is RepoServiceState.Error -> {
//                    Result.failure(Exception("Failed to get countries: ${response.error}"))
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Result.failure(Exception("Failed to get countries: ${e.message}"))
//        }
//    }
    suspend fun getCountries(): Result<LookupResponse<Country>> {
        return try {
            when (val response = repo.onGet("core-md-country/ddl")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
                        ) {
                            val dataArray = responseJson.jsonObject.getValue("data").jsonArray
                            val countries: List<Country> = json.decodeFromJsonElement(dataArray)
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
     * Get list of commercial registrations for a civil ID
     * API: GET core-simulation-companies/ddl/{civilId}
     * @param civilId The civil ID to get companies for (e.g., "12345678901234")
     */
    suspend fun getCommercialRegistrations(civilId: String): Result<LookupResponse<SelectableItem>> {
        return try {
            println("🔍 Calling API: core-simulation-companies/ddl/$civilId")

            when (val response = repo.onGet("core-simulation-companies/ddl/$civilId")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Response received")
                    println("📄 Response JSON: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        val success = responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
                        println("📊 Status Code: $statusCode, Success: $success")

                        if (statusCode == 200 && success) {
                            val dataArray = responseJson.jsonObject.getValue("data").jsonArray
                            println("📦 Companies array size: ${dataArray.size}")

                            // Parse each company item
                            val companies = dataArray.mapNotNull { companyItem ->
                                try {
                                    val obj = companyItem.jsonObject
                                    val id = obj.getValue("id").jsonPrimitive.int
                                    val companyNameAr = obj.getValue("companyNameAr").jsonPrimitive.content
                                    val companyNameEn = obj.getValue("companyNameEn").jsonPrimitive.content
                                    val crNumber = obj.getValue("crNumber").jsonPrimitive.content
                                    val companyDescription = obj.getValue("companyDescription").jsonPrimitive.content
                                    val isActive = obj.getValue("isActive").jsonPrimitive.int

                                    // Only return active companies
                                    if (isActive == 1) {
                                        SelectableItem(
                                            id = crNumber, // Use CR number as ID
                                            title = companyNameAr,
                                            code = crNumber,
                                            description = "$companyDescription\n$companyNameEn"
                                        )
                                    } else {
                                        null
                                    }
                                } catch (e: Exception) {
                                    println("⚠️ Failed to parse company: ${e.message}")
                                    e.printStackTrace()
                                    null
                                }
                            }

                            println("✅ Successfully fetched ${companies.size} commercial registrations")
                            Result.success(LookupResponse(success = true, data = companies))
                        } else {
                            println("❌ Service failed with status: $statusCode")
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
                    Result.failure(Exception("API Error: ${response.error}"))
                }

                else -> {
                    println("❌ Unknown response type")
                    Result.failure(Exception("Unknown response type"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in getCommercialRegistrations: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to get commercial registrations: ${e.message}"))
        }
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

    /**
     * Get list of mortgage reasons
     * API: mortgage-reason
     */
    suspend fun getMortgageReasons(): Result<LookupResponse<MortgageReason>> {
        return try {
            println("🔍 Calling API: mortgage-reason")
            when (val response = repo.onGet("mortgage-reason")) {
                is RepoServiceState.Success -> {
                    val raw = response.response
                    println("📥 mortgage-reason raw element type: ${raw::class.simpleName}")
                    println("📥 mortgage-reason raw: $raw")

                    // If it's a JsonArray, parse manually to avoid serialization mismatch
                    if (raw is kotlinx.serialization.json.JsonArray) {
                        val parsed = mutableListOf<MortgageReason>()
                        raw.forEach { el ->
                            try {
                                val obj = el.jsonObject
                                val id = obj.getValue("id").jsonPrimitive.int
                                val nameAr = obj.getValue("nameAr").jsonPrimitive.content
                                val nameEn = obj.getValue("nameEn").jsonPrimitive.content
                                parsed.add(
                                    MortgageReason(
                                        id = id,
                                        nameAr = nameAr,
                                        nameEn = nameEn
                                    )
                                )
                            } catch (pe: Exception) {
                                println("⚠️ Skipping invalid mortgage reason element: ${pe.message}")
                            }
                        }
                        println("✅ Manually parsed mortgage reasons from JsonArray: ${parsed.size}")
                        return Result.success(LookupResponse(true, parsed))
                    }

                    // Otherwise fall back to existing string-based decoding
                    val rawStr = raw.toString()
                    try {
                        val list: List<MortgageReason> =
                            json.decodeFromString<List<MortgageReason>>(rawStr)
                        println("✅ Parsed mortgage reasons as List (fallback): ${list.size}")
                        return Result.success(LookupResponse(true, list))
                    } catch (e: Exception) {
                        println("⚠️ Fallback decode list failed: ${e.message}")
                    }

                    try {
                        val paginated: PaginatedLookupResponse<MortgageReason> =
                            json.decodeFromString(rawStr)
                        val list = paginated.data.content
                        println("✅ Parsed mortgage reasons from PaginatedLookupResponse (fallback): ${list.size}")
                        return Result.success(LookupResponse(true, list))
                    } catch (e: Exception) {
                        println("⚠️ Fallback paginated failed: ${e.message}")
                    }

                    try {
                        val wrapped: LookupResponse<MortgageReason> = json.decodeFromString(rawStr)
                        println("✅ Parsed mortgage reasons from LookupResponse (fallback): ${wrapped.data.size}")
                        return Result.success(LookupResponse(true, wrapped.data))
                    } catch (e: Exception) {
                        println("⚠️ Fallback wrapped failed: ${e.message}")
                    }

                    println("❌ Unable to parse mortgage reasons response (after fallbacks)")
                    return Result.failure(Exception("Failed to parse mortgage reasons response"))
                }

                is RepoServiceState.Error -> {
                    println("❌ API error: ${response.error}")
                    return Result.failure(Exception("Failed to get mortgage reasons: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in getMortgageReasons: ${e.message}")
            e.printStackTrace()
            return Result.failure(Exception("Failed to get mortgage reasons: ${e.message}"))
        }
    }

    /**
     * Get list of banks
     * API: banks
     */
    suspend fun getBanks(): Result<LookupResponse<Bank>> {
        return try {
            println("🔍 Calling API: banks")
            when (val response = repo.onGet("banks")) {
                is RepoServiceState.Success -> {
                    val raw = response.response
                    println("📥 banks raw type: ${raw::class.qualifiedName}")
                    println("📥 banks raw mortgageValue: $raw")

                    try {
                        // If the API returns a wrapper object like { message, statusCode, success, timestamp, data: [...] }
                        if (raw is kotlinx.serialization.json.JsonObject) {
                            val dataElem = raw["data"]
                            if (dataElem != null) {
                                when (dataElem) {
                                    is kotlinx.serialization.json.JsonArray -> {
                                        val parsed =
                                            json.decodeFromJsonElement<List<Bank>>(dataElem)
                                        println("✅ Parsed banks from wrapper.data JsonArray: ${parsed.size}")
                                        return Result.success(LookupResponse(true, parsed))
                                    }

                                    is kotlinx.serialization.json.JsonObject -> {
                                        // sometimes data may be an object containing content array (paginated)
                                        val content = dataElem["content"]
                                        if (content is kotlinx.serialization.json.JsonArray) {
                                            val parsed =
                                                json.decodeFromJsonElement<List<Bank>>(content)
                                            println("✅ Parsed banks from wrapper.data.content JsonArray: ${parsed.size}")
                                            return Result.success(LookupResponse(true, parsed))
                                        }

                                        // fallback: try to decode dataElem as a single Bank or map entries
                                        val maybeList = try {
                                            json.decodeFromString<List<Bank>>(dataElem.toString())
                                        } catch (_: Exception) {
                                            null
                                        }
                                        if (maybeList != null) {
                                            println("✅ Parsed banks from wrapper.data as List: ${maybeList.size}")
                                            return Result.success(LookupResponse(true, maybeList))
                                        }

                                        // manual mapping if dataElem is an object mapping
                                        val manual = mutableListOf<Bank>()
                                        dataElem.forEach { (_, v) ->
                                            try {
                                                val obj = v.jsonObject
                                                val id = obj.getValue("id").jsonPrimitive.content
                                                val nameAr =
                                                    obj.getValue("nameAr").jsonPrimitive.content
                                                val nameEn =
                                                    obj.getValue("nameEn").jsonPrimitive.content
                                                manual.add(
                                                    Bank(
                                                        id = id,
                                                        nameAr = nameAr,
                                                        nameEn = nameEn
                                                    )
                                                )
                                            } catch (_: Exception) {
                                                // skip
                                            }
                                        }
                                        if (manual.isNotEmpty()) {
                                            println("✅ Manually parsed banks from wrapper.data: ${manual.size}")
                                            return Result.success(LookupResponse(true, manual))
                                        }
                                    }

                                    is kotlinx.serialization.json.JsonPrimitive -> {
                                        // data is primitive (string), try parsing its JSON
                                        val text = dataElem.toString()
                                        try {
                                            val parsedElem = json.parseToJsonElement(text)
                                            if (parsedElem is kotlinx.serialization.json.JsonArray) {
                                                val parsed = json.decodeFromJsonElement<List<Bank>>(
                                                    parsedElem
                                                )
                                                println("✅ Parsed banks from wrapper.data primitive -> JsonArray: ${parsed.size}")
                                                return Result.success(LookupResponse(true, parsed))
                                            }
                                        } catch (_: Exception) {
                                            // ignore
                                        }
                                    }

                                    else -> {
                                        // unknown dataElem type
                                    }
                                }
                            }

                            // Try fallback: decode the whole object as LookupResponse<Bank>
                            try {
                                val wrapped: LookupResponse<Bank> =
                                    json.decodeFromString(raw.toString())
                                println("✅ Parsed banks from wrapper as LookupResponse: ${wrapped.data.size}")
                                return Result.success(LookupResponse(true, wrapped.data))
                            } catch (_: Exception) {
                                // continue to other fallbacks
                            }
                        }

                        // If the response is a JsonArray directly
                        if (raw is kotlinx.serialization.json.JsonArray) {
                            val parsed = json.decodeFromJsonElement<List<Bank>>(raw)
                            println("✅ Parsed banks from JsonArray: ${parsed.size}")
                            return Result.success(LookupResponse(true, parsed))
                        }

                        // If raw is a primitive/string containing JSON
                        try {
                            val rawStr = raw.toString()

                            // Try several fallbacks: List<Bank>, PaginatedLookupResponse, LookupResponse
                            try {
                                val list: List<Bank> = json.decodeFromString(rawStr)
                                println("✅ Parsed banks as List (fallback): ${list.size}")
                                return Result.success(LookupResponse(true, list))
                            } catch (_: Exception) {
                            }

                            try {
                                val paginated: PaginatedLookupResponse<Bank> =
                                    json.decodeFromString(rawStr)
                                val list = paginated.data.content
                                println("✅ Parsed banks from PaginatedLookupResponse (fallback): ${list.size}")
                                return Result.success(LookupResponse(true, list))
                            } catch (_: Exception) {
                            }

                            try {
                                val wrapped: LookupResponse<Bank> = json.decodeFromString(rawStr)
                                println("✅ Parsed banks from LookupResponse (fallback): ${wrapped.data.size}")
                                return Result.success(LookupResponse(true, wrapped.data))
                            } catch (_: Exception) {
                            }

                        } catch (_: Exception) {
                            // ignore
                        }

                        println("❌ Unable to parse banks response (after fallbacks)")
                        return Result.failure(Exception("Failed to parse banks response - unexpected format"))

                    } catch (e: Exception) {
                        println("❌ Exception while parsing banks response: ${e.message}")
                        e.printStackTrace()
                        return Result.failure(Exception("Failed to parse banks response: ${e.message}"))
                    }
                }

                is RepoServiceState.Error -> {
                    println("❌ API error: ${response.error}")
                    return Result.failure(Exception("Failed to get banks: ${response.error}"))
                }

                else -> return Result.failure(Exception("Failed to get banks: unknown repo state"))
            }
        } catch (e: Exception) {
            println("❌ Exception in getBanks: ${e.message}")
            e.printStackTrace()
            return Result.failure(Exception("Failed to get banks: ${e.message}"))
        }
    }

    /**
     * Get list of navigation areas (sailing regions)
     * API: ship-navigation-license-request/navigation-areas
     */
    suspend fun getNavigationAreas(): Result<LookupResponse<NavigationArea>> {
        return try {
            when (val response = repo.onGet("ship-navigation-license-request/navigation-areas")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
                        ) {

                            val dataArray = responseJson.jsonObject.getValue("data").jsonArray
                            val areas: List<NavigationArea> = json.decodeFromJsonElement(dataArray)

                            Result.success(LookupResponse(true, areas))
                        } else {
                            Result.failure(Exception("Failed to fetch navigation areas"))
                        }
                    } else {
                        Result.failure(Exception("Empty navigation areas response"))
                    }
                }

                is RepoServiceState.Error -> {
                    Result.failure(Exception("Failed to get navigation areas: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to get navigation areas: ${e.message}"))
        }
    }

    /**
     * Get list of crew job titles
     * API: crew-job-title
     */
    suspend fun getCrewJobTitles(): Result<LookupResponse<CrewJobTitle>> {
        return try {
            when (val response = repo.onGet("crew-job-title")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
                        ) {

                            val dataElem = responseJson.jsonObject.getValue("data")
                            // data can be either an array or a paginated object - try array first
                            if (dataElem is kotlinx.serialization.json.JsonArray) {
                                val items: List<CrewJobTitle> = json.decodeFromJsonElement(dataElem)
                                return Result.success(LookupResponse(true, items))
                            }

                            // If it's an object with content
                            if (dataElem is kotlinx.serialization.json.JsonObject) {
                                val content = dataElem["content"]
                                if (content is kotlinx.serialization.json.JsonArray) {
                                    val items: List<CrewJobTitle> = json.decodeFromJsonElement(content)
                                    return Result.success(LookupResponse(true, items))
                                }
                            }

                            // fallback: try decode data as List from string
                            try {
                                val rawStr = dataElem.toString()
                                val list: List<CrewJobTitle> = json.decodeFromString(rawStr)
                                return Result.success(LookupResponse(true, list))
                            } catch (_: Exception) {
                                // ignore
                            }

                            return Result.failure(Exception("Failed to fetch crew job titles - unexpected data format"))
                        } else {
                            Result.failure(Exception("Failed to fetch crew job titles"))
                        }
                    } else {
                        Result.failure(Exception("Empty crew job titles response"))
                    }
                }

                is RepoServiceState.Error -> {
                    Result.failure(Exception("Failed to get crew job titles: ${'$'}{response.error}"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to get crew job titles: ${'$'}{e.message}"))
        }
    }

    /**
     * Get required documents for a specific request type
     * API: reqtype/{requestTypeId}/documents
     *
     * @param requestTypeId The ID of the request type
     * @return Result containing list of required documents
     */
    suspend fun getRequiredDocumentsByRequestType(requestTypeId: String): Result<RequiredDocumentsResponse> {
        return try {
            when (val response = repo.onGet("reqtype/$requestTypeId/documents")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response

                    // ✅ DEBUG: Print the raw JSON response
                    println("🔍 RAW JSON RESPONSE for required documents:")
                    println(responseJson.toString())

                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
                        ) {
                            // ✅ DEBUG: Print the data array
                            println("🔍 DATA ARRAY:")
                            println(responseJson.jsonObject["data"].toString())

                            val documentsResponse: RequiredDocumentsResponse =
                                json.decodeFromJsonElement(responseJson)
                            Result.success(documentsResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to fetch required documents"
                            Result.failure(Exception(message))
                        }
                    } else {
                        Result.failure(Exception("Empty required documents response"))
                    }
                }

                is RepoServiceState.Error -> {
                    Result.failure(Exception("Failed to get required documents: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to get required documents: ${e.message}"))
        }
    }

    // ✅ NEW: Get inspection purposes
    suspend fun getInspectionPurposes(): Result<LookupResponse<InspectionPurpose>> {
        return try {
            when (val response = repo.onGet("inspection-requests/purposes")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
                        ) {
                            val dataArray = responseJson.jsonObject.getValue("data").jsonArray
                            val purposes: List<InspectionPurpose> = json.decodeFromJsonElement(dataArray)
                            Result.success(LookupResponse(true, purposes))
                        } else {
                            Result.failure(Exception("Failed to fetch inspection purposes"))
                        }
                    } else {
                        Result.failure(Exception("Empty inspection purposes response"))
                    }
                }
                is RepoServiceState.Error -> {
                    Result.failure(Exception("Failed to get inspection purposes: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to get inspection purposes: ${e.message}"))
        }
    }

    // ✅ NEW: Get inspection authorities by ship info ID
    suspend fun getInspectionAuthorities(shipInfoId: Int): Result<LookupResponse<InspectionAuthority>> {
        return try {
            when (val response = repo.onGet("inspection-requests/authorities/$shipInfoId")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        if (responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && responseJson.jsonObject.getValue("success").jsonPrimitive.boolean
                        ) {
                            val dataArray = responseJson.jsonObject.getValue("data").jsonArray
                            val authorities: List<InspectionAuthority> = json.decodeFromJsonElement(dataArray)
                            Result.success(LookupResponse(true, authorities))
                        } else {
                            Result.failure(Exception("Failed to fetch inspection authorities"))
                        }
                    } else {
                        Result.failure(Exception("Empty inspection authorities response"))
                    }
                }
                is RepoServiceState.Error -> {
                    Result.failure(Exception("Failed to get inspection authorities: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to get inspection authorities: ${e.message}"))
        }
    }
}
