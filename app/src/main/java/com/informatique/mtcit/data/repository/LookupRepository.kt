package com.informatique.mtcit.data.repository

import com.informatique.mtcit.data.api.LookupApiService
import com.informatique.mtcit.data.model.City
import com.informatique.mtcit.data.model.Country
import com.informatique.mtcit.data.model.Port
import com.informatique.mtcit.data.model.ShipType
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
    suspend fun getCitiesByCountry(countryId: String): Result<List<String>>

    suspend fun getCommercialRegistrations(): Result<List<SelectableItem>>

    suspend fun getPersonTypes(): Result<List<PersonType>>
}

@Singleton
class LookupRepositoryImpl @Inject constructor(
    private val apiService: LookupApiService
) : LookupRepository {

    // Simple in-memory cache
    private var cachedPorts: List<Port>? = null
    private var cachedCountries: List<Country>? = null
    private var cachedShipTypes: List<ShipType>? = null
    private val cachedCities = mutableMapOf<String, List<City>>()

    private var cachedCommercialRegistrations: List<SelectableItem>? = null

    private var cachedPersonTypes: List<PersonType>? = null

    override suspend fun getPorts(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            if (cachedPorts != null) {
                return@withContext Result.success(cachedPorts!!.map { getLocalizedName(it.nameAr, it.nameEn) })
            }

            // Fetch from API
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
            // Check cache first
            if (cachedCountries != null) {
                return@withContext Result.success(cachedCountries!!.map { getLocalizedName(it.nameAr, it.nameEn) })
            }

            // Fetch from API
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
            // Check cache first
            if (cachedShipTypes != null) {
                return@withContext Result.success(cachedShipTypes!!.map { getLocalizedName(it.nameAr, it.nameEn) })
            }

            // Fetch from API
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

    override suspend fun getCitiesByCountry(countryId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            if (cachedCities.containsKey(countryId)) {
                return@withContext Result.success(cachedCities[countryId]!!.map { getLocalizedName(it.nameAr, it.nameEn) })
            }

            // Fetch from API
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
            // Check cache first
            if (cachedCommercialRegistrations != null) {
                return@withContext Result.success(cachedCommercialRegistrations!!)
            }

            // Fetch from API
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
            // Check cache first
            if (cachedPersonTypes != null) {
                return@withContext Result.success(cachedPersonTypes!!)
            }

            // Fetch from API
            val result = apiService.getPersonTypes()

            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedPersonTypes = response.data
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

    /**
     * Get localized name based on current language
     */
    private fun getLocalizedName(arabicName: String, englishName: String): String {
        val currentLanguage = Locale.getDefault().language
        return if (currentLanguage == "ar") arabicName else englishName
    }

    /**
     * Clear all cached data
     */
    fun clearCache() {
        cachedPorts = null
        cachedCountries = null
        cachedShipTypes = null
        cachedCities.clear()
    }
}
