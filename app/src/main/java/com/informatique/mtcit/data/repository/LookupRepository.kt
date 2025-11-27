package com.informatique.mtcit.data.repository

import com.informatique.mtcit.data.api.LookupApiService
import com.informatique.mtcit.data.model.*
import com.informatique.mtcit.ui.components.PersonType
import com.informatique.mtcit.ui.components.SelectableItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for fetching lookup/dropdown data
 */
interface LookupRepository {
    suspend fun getPorts(): Result<List<String>>
    suspend fun getCountries(): Result<List<String>>
    suspend fun getShipTypes(): Result<List<String>>
    suspend fun getShipTypesByCategory(categoryId: Int): Result<List<String>>
    suspend fun getShipCategories(): Result<List<String>>
    suspend fun getEngineStatuses(): Result<List<String>>
    suspend fun getProofTypes(): Result<List<String>>
    suspend fun getMarineActivities(): Result<List<String>>
    suspend fun getBuildMaterials(): Result<List<String>>
    suspend fun getCitiesByCountry(countryId: String): Result<List<String>>

    suspend fun getCommercialRegistrations(): Result<List<SelectableItem>>

    suspend fun getPersonTypes(): Result<List<PersonType>>

    // NEW: Get category ID from category name for cascading dropdowns
    fun getShipCategoryId(categoryName: String): Int?

    fun clearCache()
}

@Singleton
class LookupRepositoryImpl @Inject constructor(
    private val apiService: LookupApiService
) : LookupRepository {

    // In-memory cache that persists until app is closed
    private var cachedPorts: List<Port>? = null
    private var cachedCountries: List<Country>? = null
    private var cachedShipTypes: List<ShipType>? = null
    private var cachedShipCategories: List<ShipCategory>? = null
    private var cachedEngineStatuses: List<EngineStatus>? = null
    private var cachedProofTypes: List<ProofType>? = null
    private var cachedMarineActivities: List<MarineActivity>? = null
    private var cachedBuildMaterials: List<BuildMaterial>? = null
    private val cachedShipTypesByCategory = mutableMapOf<Int, List<ShipType>>()
    private val cachedCities = mutableMapOf<String, List<City>>()
    private var cachedCommercialRegistrations: List<SelectableItem>? = null
    private var cachedPersonTypes: List<PersonType>? = null

    override suspend fun getPorts(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedPorts != null) {
                return@withContext Result.success(cachedPorts!!.map { getLocalizedName(it.nameAr, it.nameEn) })
            }

            val result = apiService.getPorts()
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedPorts = response.data
                        Result.success(response.data.map { getLocalizedName(it.nameAr, it.nameEn) })
                    } else {
                        Result.failure(Exception(response.message ?: "Failed to fetch ports"))
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCountries(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedCountries != null) {
                return@withContext Result.success(cachedCountries!!.map { getLocalizedName(it.nameAr, it.nameEn) })
            }

            val result = apiService.getCountries()
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedCountries = response.data
                        Result.success(response.data.map { getLocalizedName(it.nameAr, it.nameEn) })
                    } else {
                        Result.failure(Exception(response.message ?: "Failed to fetch countries"))
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getShipTypes(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedShipTypes != null) {
                return@withContext Result.success(cachedShipTypes!!.map { getLocalizedName(it.nameAr, it.nameEn) })
            }

            val result = apiService.getShipTypes()
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedShipTypes = response.data
                        Result.success(response.data.map { getLocalizedName(it.nameAr, it.nameEn) })
                    } else {
                        Result.failure(Exception(response.message ?: "Failed to fetch ship types"))
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getShipTypesByCategory(categoryId: Int): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedShipTypesByCategory.containsKey(categoryId)) {
                return@withContext Result.success(
                    cachedShipTypesByCategory[categoryId]!!.map { getLocalizedName(it.nameAr, it.nameEn) }
                )
            }

            val result = apiService.getShipTypesByCategory(categoryId)
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedShipTypesByCategory[categoryId] = response.data
                        Result.success(response.data.map { getLocalizedName(it.nameAr, it.nameEn) })
                    } else {
                        Result.failure(Exception(response.message ?: "Failed to fetch ship types by category"))
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getShipCategories(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedShipCategories != null) {
                return@withContext Result.success(cachedShipCategories!!.map { getLocalizedName(it.nameAr, it.nameEn) })
            }

            val result = apiService.getShipCategories()
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedShipCategories = response.data
                        Result.success(response.data.map { getLocalizedName(it.nameAr, it.nameEn) })
                    } else {
                        Result.failure(Exception(response.message ?: "Failed to fetch ship categories"))
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEngineStatuses(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedEngineStatuses != null) {
                return@withContext Result.success(cachedEngineStatuses!!.map { getLocalizedName(it.nameAr, it.nameEn) })
            }

            val result = apiService.getEngineStatuses()
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedEngineStatuses = response.data
                        Result.success(response.data.map { getLocalizedName(it.nameAr, it.nameEn) })
                    } else {
                        Result.failure(Exception(response.message ?: "Failed to fetch engine statuses"))
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProofTypes(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedProofTypes != null) {
                return@withContext Result.success(cachedProofTypes!!.map { getLocalizedName(it.nameAr, it.nameEn) })
            }

            val result = apiService.getProofTypes()
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedProofTypes = response.data
                        Result.success(response.data.map { getLocalizedName(it.nameAr, it.nameEn) })
                    } else {
                        Result.failure(Exception(response.message ?: "Failed to fetch proof types"))
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMarineActivities(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedMarineActivities != null) {
                return@withContext Result.success(cachedMarineActivities!!.map { getLocalizedName(it.nameAr, it.nameEn) })
            }

            val result = apiService.getMarineActivities()
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedMarineActivities = response.data
                        Result.success(response.data.map { getLocalizedName(it.nameAr, it.nameEn) })
                    } else {
                        Result.failure(Exception(response.message ?: "Failed to fetch marine activities"))
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBuildMaterials(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedBuildMaterials != null) {
                return@withContext Result.success(cachedBuildMaterials!!.map { getLocalizedName(it.nameAr, it.nameEn) })
            }

            val result = apiService.getBuildMaterials()
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedBuildMaterials = response.data
                        Result.success(response.data.map { getLocalizedName(it.nameAr, it.nameEn) })
                    } else {
                        Result.failure(Exception(response.message ?: "Failed to fetch build materials"))
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCitiesByCountry(countryId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedCities.containsKey(countryId)) {
                return@withContext Result.success(cachedCities[countryId]!!.map { getLocalizedName(it.nameAr, it.nameEn) })
            }

            val result = apiService.getCitiesByCountry(countryId)
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedCities[countryId] = response.data
                        Result.success(response.data.map { getLocalizedName(it.nameAr, it.nameEn) })
                    } else {
                        Result.failure(Exception(response.message ?: "Failed to fetch cities"))
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCommercialRegistrations(): Result<List<SelectableItem>> = withContext(Dispatchers.IO) {
        try {
            if (cachedCommercialRegistrations != null) {
                return@withContext Result.success(cachedCommercialRegistrations!!)
            }

            val result = apiService.getCommercialRegistrations()
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedCommercialRegistrations = response.data
                        Result.success(response.data)
                    } else {
                        Result.failure(Exception(response.message ?: "Failed to fetch commercial registrations"))
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPersonTypes(): Result<List<PersonType>> = withContext(Dispatchers.IO) {
        try {
            if (cachedPersonTypes != null) {
                return@withContext Result.success(cachedPersonTypes!!)
            }

            val result = apiService.getPersonTypes()
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedPersonTypes = response.data
                        Result.success(response.data)
                    } else {
                        Result.failure(Exception(response.message ?: "Failed to fetch person types"))
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get localized name based on current language
     */
    private fun getLocalizedName(arabicName: String, englishName: String): String {
        val currentLanguage = Locale.getDefault().language
        return if (currentLanguage == "ar") arabicName else englishName
    }

    /**
     * Clear all cached data (called when app closes or user logs out)
     */
    override fun clearCache() {
        cachedPorts = null
        cachedCountries = null
        cachedShipTypes = null
        cachedShipCategories = null
        cachedEngineStatuses = null
        cachedProofTypes = null
        cachedMarineActivities = null
        cachedBuildMaterials = null
        cachedShipTypesByCategory.clear()
        cachedCities.clear()
        cachedCommercialRegistrations = null
        cachedPersonTypes = null
    }

    /**
     * Get ship category ID from category name
     */
    override fun getShipCategoryId(categoryName: String): Int? {
        return cachedShipCategories?.find { getLocalizedName(it.nameAr, it.nameEn) == categoryName }?.id
    }
}
