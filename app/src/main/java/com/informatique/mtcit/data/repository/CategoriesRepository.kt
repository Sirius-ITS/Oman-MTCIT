package com.informatique.mtcit.data.repository

import com.informatique.mtcit.ui.models.MainCategory
import com.informatique.mtcit.ui.models.getMainCategories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for fetching categories data
 */
interface CategoriesRepository {
    suspend fun getCategories(): Result<List<MainCategory>>
}

@Singleton
class CategoriesRepositoryImpl @Inject constructor(
    // TODO: Inject API service when endpoint is ready
    // private val apiService: CategoriesApiService
) : CategoriesRepository {

    // Cache for categories
    private var cachedCategories: List<MainCategory>? = null

    override suspend fun getCategories(): Result<List<MainCategory>> = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            if (cachedCategories != null) {
                return@withContext Result.success(cachedCategories!!)
            }

            // Simulate API delay
            delay(300)

            // TODO: Replace with actual API call when endpoint is ready
            // val result = apiService.getCategories()
            // result.fold(
            //     onSuccess = { response ->
            //         if (response.success) {
            //             cachedCategories = response.data
            //             Result.success(response.data)
            //         } else {
            //             Result.failure(Exception(response.message ?: "Failed to fetch categories"))
            //         }
            //     },
            //     onFailure = { error ->
            //         Result.failure(error)
            //     }
            // )

            // Mock implementation - get from local data source
            val categories = getMainCategories()
            cachedCategories = categories
            Result.success(categories)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clear cache - useful for refresh
     */
    fun clearCache() {
        cachedCategories = null
    }
}
