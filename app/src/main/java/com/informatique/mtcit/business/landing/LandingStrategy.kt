package com.informatique.mtcit.business.landing

import com.informatique.mtcit.data.repository.LandingRepository
import com.informatique.mtcit.ui.models.MainCategory
import javax.inject.Inject

/**
 * Strategy interface for Landing operations
 * Responsible for initial app setup and loading categories
 */
interface LandingStrategyInterface {
    suspend fun loadCategories(): Result<List<MainCategory>>
}

/**
 * Implementation of Landing Strategy
 * Handles loading categories at app startup
 */
class LandingStrategy @Inject constructor(
    private val repository: LandingRepository
) : LandingStrategyInterface {

    override suspend fun loadCategories(): Result<List<MainCategory>> {
        return try {
            repository.getCategories()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

