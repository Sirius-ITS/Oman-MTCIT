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
    suspend fun getEngineTypes(): Result<List<String>>
    suspend fun getEngineStatuses(): Result<List<String>>
    suspend fun getEngineFuelTypes(): Result<List<String>>
    suspend fun getProofTypes(): Result<List<String>>
    suspend fun getMarineActivities(): Result<List<String>>
    suspend fun getBuildMaterials(): Result<List<String>>
    suspend fun getNavigationAreas(): Result<List<NavigationArea>>
    suspend fun getCrewJobTitles(): Result<List<String>>
    suspend fun getCitiesByCountry(countryId: String): Result<List<String>>
    suspend fun getMortgageReasons(): Result<List<String>>
    suspend fun getBanks(): Result<List<String>>

    suspend fun getCommercialRegistrations(civilId: String): Result<List<SelectableItem>>

    suspend fun getPersonTypes(): Result<List<PersonType>>

    // NEW: Get category ID from category name for cascading dropdowns
    fun getShipCategoryId(categoryName: String): Int?

    // ✅ NEW: Get IDs from names for all lookups (for mapper usage)
    fun getPortId(portName: String): String?
    fun getMarineActivityId(activityName: String): Int?
    fun getShipTypeId(typeName: String): Int?
    fun getProofTypeId(proofTypeName: String): Int?
    fun getCountryId(countryName: String): String?
    fun getBuildMaterialId(materialName: String): Int?

    // Crew job titles
    fun getCrewJobTitleId(jobName: String): Int?

    // ✅ NEW: Get raw lookup objects (with id, nameEn, nameAr) for engine submission
    suspend fun getEngineTypesRaw(): List<EngineType>
    suspend fun getCountriesRaw(): List<Country>
    suspend fun getFuelTypesRaw(): List<FuelType>
    suspend fun getEngineStatusesRaw(): List<EngineStatus>
    suspend fun getCrewJobTitlesRaw(): List<CrewJobTitle>

    // Get IDs from cached data for API submissions
    fun getBankId(bankName: String): Int?
    fun getMortgageReasonId(reasonName: String): Int?

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
    private var cachedEngineTypes: List<EngineType>? = null
    private var cachedEngineStatuses: List<EngineStatus>? = null
    private var cachedEngineFuelTypes: List<FuelType>? = null
    private var cachedProofTypes: List<ProofType>? = null
    private var cachedMarineActivities: List<MarineActivity>? = null
    private var cachedBuildMaterials: List<BuildMaterial>? = null
    private var cachedNavigationAreas: List<NavigationArea>? = null
    private var cachedMortgageReasons: List<MortgageReason>? = null
    private var cachedBanks: List<Bank>? = null
    private val cachedShipTypesByCategory = mutableMapOf<Int, List<ShipType>>()
    private val cachedCities = mutableMapOf<String, List<City>>()
    private var cachedCommercialRegistrations: List<SelectableItem>? = null
    private var cachedPersonTypes: List<PersonType>? = null
    private var cachedCrewJobTitles: List<CrewJobTitle>? = null

    override suspend fun getPorts(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedPorts != null) {
                return@withContext Result.success(cachedPorts!!.map {
                    getLocalizedName(
                        it.nameAr,
                        it.nameEn
                    )
                })
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
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getCountries(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedCountries != null) {
                return@withContext Result.success(cachedCountries!!.map {
                    getLocalizedName(
                        it.nameAr,
                        it.nameEn
                    )
                })
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
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getShipTypes(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedShipTypes != null) {
                return@withContext Result.success(cachedShipTypes!!.map {
                    getLocalizedName(
                        it.nameAr,
                        it.nameEn
                    )
                })
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
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getShipTypesByCategory(categoryId: Int): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                if (cachedShipTypesByCategory.containsKey(categoryId)) {
                    return@withContext Result.success(
                        cachedShipTypesByCategory[categoryId]!!.map {
                            getLocalizedName(
                                it.nameAr,
                                it.nameEn
                            )
                        }
                    )
                }

                val result = apiService.getShipTypesByCategory(categoryId)
                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            cachedShipTypesByCategory[categoryId] = response.data
                            Result.success(response.data.map {
                                getLocalizedName(
                                    it.nameAr,
                                    it.nameEn
                                )
                            })
                        } else {
                            Result.failure(
                                Exception(
                                    response.message ?: "Failed to fetch ship types by category"
                                )
                            )
                        }
                    },
                    onFailure = { exception ->
                        Result.failure(exception)
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    override suspend fun getShipCategories(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedShipCategories != null) {
                return@withContext Result.success(cachedShipCategories!!.map {
                    getLocalizedName(
                        it.nameAr,
                        it.nameEn
                    )
                })
            }

            val result = apiService.getShipCategories()
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedShipCategories = response.data
                        Result.success(response.data.map { getLocalizedName(it.nameAr, it.nameEn) })
                    } else {
                        Result.failure(
                            Exception(
                                response.message ?: "Failed to fetch ship categories"
                            )
                        )
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getEngineTypes(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedEngineTypes != null) {
                return@withContext Result.success(cachedEngineTypes!!.map {
                    getLocalizedName(
                        it.nameAr,
                        it.nameEn
                    )
                })
            }

            val result = apiService.getEngineTypes()
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedEngineTypes = response.data
                        Result.success(response.data.map { getLocalizedName(it.nameAr, it.nameEn) })
                    } else {
                        Result.failure(
                            Exception(
                                response.message ?: "Failed to fetch engine types"
                            )
                        )
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getEngineStatuses(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedEngineStatuses != null) {
                return@withContext Result.success(cachedEngineStatuses!!.map {
                    getLocalizedName(
                        it.nameAr,
                        it.nameEn
                    )
                })
            }

            val result = apiService.getEngineStatuses()
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedEngineStatuses = response.data
                        Result.success(response.data.map { getLocalizedName(it.nameAr, it.nameEn) })
                    } else {
                        Result.failure(
                            Exception(
                                response.message ?: "Failed to fetch engine statuses"
                            )
                        )
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getEngineFuelTypes(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedEngineFuelTypes != null) {
                return@withContext Result.success(cachedEngineFuelTypes!!.map {
                    getLocalizedName(
                        it.nameAr,
                        it.nameEn
                    )
                })
            }

            val result = apiService.getEngineFuelTypes()
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedEngineFuelTypes = response.data
                        Result.success(response.data.map { getLocalizedName(it.nameAr, it.nameEn) })
                    } else {
                        Result.failure(Exception(response.message ?: "Failed to fetch fuel types"))
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getProofTypes(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedProofTypes != null) {
                return@withContext Result.success(cachedProofTypes!!.map {
                    getLocalizedName(
                        it.nameAr,
                        it.nameEn
                    )
                })
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
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getMarineActivities(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedMarineActivities != null) {
                return@withContext Result.success(cachedMarineActivities!!.map {
                    getLocalizedName(
                        it.nameAr,
                        it.nameEn
                    )
                })
            }

            val result = apiService.getMarineActivities()
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedMarineActivities = response.data
                        Result.success(response.data.map { getLocalizedName(it.nameAr, it.nameEn) })
                    } else {
                        Result.failure(
                            Exception(
                                response.message ?: "Failed to fetch marine activities"
                            )
                        )
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getBuildMaterials(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedBuildMaterials != null) {
                return@withContext Result.success(cachedBuildMaterials!!.map {
                    getLocalizedName(
                        it.nameAr,
                        it.nameEn
                    )
                })
            }

            val result = apiService.getBuildMaterials()
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedBuildMaterials = response.data
                        Result.success(response.data.map { getLocalizedName(it.nameAr, it.nameEn) })
                    } else {
                        Result.failure(
                            Exception(
                                response.message ?: "Failed to fetch build materials"
                            )
                        )
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getNavigationAreas(): Result<List<NavigationArea>> = withContext(Dispatchers.IO) {
        try {
            if (cachedNavigationAreas != null) {
                return@withContext Result.success(cachedNavigationAreas!!)
            }

            val result = apiService.getNavigationAreas()
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedNavigationAreas = response.data
                        Result.success(response.data)
                    } else {
                        Result.failure(
                            Exception(
                                response.message ?: "Failed to fetch navigation areas"
                            )
                        )
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getCrewJobTitles(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedCrewJobTitles != null) {
                return@withContext Result.success(cachedCrewJobTitles!!.map { getLocalizedName(it.nameAr, it.nameEn) })
            }

            val result = apiService.getCrewJobTitles()
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedCrewJobTitles = response.data
                        Result.success(response.data.map { getLocalizedName(it.nameAr, it.nameEn) })
                    } else {
                        Result.failure(Exception(response.message ?: "Failed to fetch crew job titles"))
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getCitiesByCountry(countryId: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                if (cachedCities.containsKey(countryId)) {
                    return@withContext Result.success(cachedCities[countryId]!!.map {
                        getLocalizedName(
                            it.nameAr,
                            it.nameEn
                        )
                    })
                }

                val result = apiService.getCitiesByCountry(countryId)
                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            cachedCities[countryId] = response.data
                            Result.success(response.data.map {
                                getLocalizedName(
                                    it.nameAr,
                                    it.nameEn
                                )
                            })
                        } else {
                            Result.failure(Exception(response.message ?: "Failed to fetch cities"))
                        }
                    },
                    onFailure = { exception ->
                        Result.failure(exception)
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    override suspend fun getCommercialRegistrations(civilId: String): Result<List<SelectableItem>> =
        withContext(Dispatchers.IO) {
            try {
                // ✅ Cache based on civilId to allow different users
                if (cachedCommercialRegistrations != null) {
                    println("📦 Using cached commercial registrations")
                    return@withContext Result.success(cachedCommercialRegistrations!!)
                }

                println("🔍 Fetching commercial registrations for civilId: $civilId")
                val result = apiService.getCommercialRegistrations(civilId)
                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            cachedCommercialRegistrations = response.data
                            Result.success(response.data)
                        } else {
                            Result.failure(
                                Exception(
                                    response.message ?: "Failed to fetch commercial registrations"
                                )
                            )
                        }
                    },
                    onFailure = { exception ->
                        Result.failure(exception)
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
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
                        Result.failure(
                            Exception(
                                response.message ?: "Failed to fetch person types"
                            )
                        )
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getMortgageReasons(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedMortgageReasons != null) {
                return@withContext Result.success(cachedMortgageReasons!!.map {
                    getLocalizedName(
                        it.nameAr,
                        it.nameEn
                    )
                })
            }

            val result = apiService.getMortgageReasons()
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedMortgageReasons = response.data
                        Result.success(response.data.map { getLocalizedName(it.nameAr, it.nameEn) })
                    } else {
                        Result.failure(
                            Exception(
                                response.message ?: "Failed to fetch mortgage reasons"
                            )
                        )
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getBanks(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (cachedBanks != null) {
                return@withContext Result.success(cachedBanks!!.map {
                    getLocalizedName(
                        it.nameAr,
                        it.nameEn
                    )
                })
            }

            val result = apiService.getBanks()
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cachedBanks = response.data
                        Result.success(response.data.map { getLocalizedName(it.nameAr, it.nameEn) })
                    } else {
                        Result.failure(Exception(response.message ?: "Failed to fetch banks"))
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
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
        cachedNavigationAreas = null
        cachedMortgageReasons = null
        cachedBanks = null
        cachedShipTypesByCategory.clear()
        cachedCities.clear()
        cachedCommercialRegistrations = null
        cachedPersonTypes = null
        cachedCrewJobTitles = null
    }

    /**
     * Get ship category ID from category name
     */
    override fun getShipCategoryId(categoryName: String): Int? {
        return cachedShipCategories?.find {
            getLocalizedName(
                it.nameAr,
                it.nameEn
            ) == categoryName
        }?.id
    }

    /**
     * Get IDs from names for all lookups (for mapper usage)
     */
    override fun getPortId(portName: String): String? {
        return cachedPorts?.find { getLocalizedName(it.nameAr, it.nameEn) == portName }?.id
    }

    override fun getMarineActivityId(activityName: String): Int? {
        return cachedMarineActivities?.find { getLocalizedName(it.nameAr, it.nameEn) == activityName }?.id
    }

    override fun getShipTypeId(typeName: String): Int? {
        return cachedShipTypes?.find { getLocalizedName(it.nameAr, it.nameEn) == typeName }?.id
    }

    override fun getProofTypeId(proofTypeName: String): Int? {
        return cachedProofTypes?.find { getLocalizedName(it.nameAr, it.nameEn) == proofTypeName }?.id
    }

    override fun getCountryId(countryName: String): String? {
        return cachedCountries?.find { getLocalizedName(it.nameAr, it.nameEn) == countryName }?.id
    }

    override fun getBuildMaterialId(materialName: String): Int? {
        return cachedBuildMaterials?.find { getLocalizedName(it.nameAr, it.nameEn) == materialName }?.id
    }

    override fun getCrewJobTitleId(jobName: String): Int? {
        return cachedCrewJobTitles?.find { getLocalizedName(it.nameAr, it.nameEn) == jobName }?.id
    }

    /**
     * Get raw lookup objects (with id, nameEn, nameAr) for engine submission
     */
    override suspend fun getEngineTypesRaw(): List<EngineType> {
        return withContext(Dispatchers.IO) {
            try {
                // Use cached data if available
                if (cachedEngineTypes != null) {
                    return@withContext cachedEngineTypes!!
                }

                // Otherwise, fetch from API
                val result = apiService.getEngineTypes()
                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            cachedEngineTypes = response.data
                            response.data
                        } else {
                            emptyList()
                        }
                    },
                    onFailure = {
                        emptyList()
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    override suspend fun getCountriesRaw(): List<Country> {
        return withContext(Dispatchers.IO) {
            try {
                // Use cached data if available
                if (cachedCountries != null) {
                    return@withContext cachedCountries!!
                }

                // Otherwise, fetch from API
                val result = apiService.getCountries()
                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            cachedCountries = response.data
                            response.data
                        } else {
                            emptyList()
                        }
                    },
                    onFailure = {
                        emptyList()
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    override suspend fun getFuelTypesRaw(): List<FuelType> {
        return withContext(Dispatchers.IO) {
            try {
                // Use cached data if available
                if (cachedEngineFuelTypes != null) {
                    return@withContext cachedEngineFuelTypes!!
                }

                // Otherwise, fetch from API
                val result = apiService.getEngineFuelTypes()
                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            cachedEngineFuelTypes = response.data
                            response.data
                        } else {
                            emptyList()
                        }
                    },
                    onFailure = {
                        emptyList()
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    override suspend fun getEngineStatusesRaw(): List<EngineStatus> {
        return withContext(Dispatchers.IO) {
            try {
                if (cachedEngineStatuses != null) {
                    return@withContext cachedEngineStatuses!!
                }

                val result = apiService.getEngineStatuses()
                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            cachedEngineStatuses = response.data
                            response.data
                        } else {
                            emptyList()
                        }
                    },
                    onFailure = {
                        emptyList()
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    override suspend fun getCrewJobTitlesRaw(): List<CrewJobTitle> {
        return withContext(Dispatchers.IO) {
            try {
                if (cachedCrewJobTitles != null) return@withContext cachedCrewJobTitles!!
                val result = apiService.getCrewJobTitles()
                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            cachedCrewJobTitles = response.data
                            response.data
                        } else {
                            emptyList()
                        }
                    },
                    onFailure = { ex ->
                        ex.printStackTrace()
                        emptyList()
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    override fun getBankId(bankName: String): Int? {
        val bank = cachedBanks?.find { getLocalizedName(it.nameAr, it.nameEn) == bankName }
        return bank?.id?.toIntOrNull().also {
            if (it == null && bank != null) {
                println("⚠️ Warning: Could not convert bank ID '${bank.id}' to Int for bank: $bankName")
            }
        }
    }

    override fun getMortgageReasonId(reasonName: String): Int? {
        return cachedMortgageReasons?.find { getLocalizedName(it.nameAr, it.nameEn) == reasonName }?.id
    }
}
