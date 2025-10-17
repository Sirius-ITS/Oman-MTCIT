package com.informatique.mtcit.business.home

import javax.inject.Inject
import javax.inject.Provider

/**
 * Factory for creating home strategies
 * Uses Hilt's Provider to lazily create strategies only when needed
 */
class HomeStrategyFactory @Inject constructor(
    private val categoriesStrategy: Provider<CategoriesStrategy>
) {

    /**
     * Create a strategy instance for the given home feature
     */
    fun createCategoriesStrategy(): HomeStrategy {
        return categoriesStrategy.get()
    }
}

