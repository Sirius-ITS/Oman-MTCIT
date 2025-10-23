package com.informatique.mtcit.business.home

import javax.inject.Inject
import javax.inject.Provider

/**
 * Factory for creating home strategies
 * Uses Hilt's Provider to lazily create strategies only when needed
 */
class MainCategoriesStrategyFactory @Inject constructor(
    private val categoriesStrategy: Provider<MainCategoriesStrategy>
) {

    /**
     * Create a strategy instance for the given home feature
     */
    fun createCategoriesStrategy(): MainCategoriesStrategyInterface {
        return categoriesStrategy.get()
    }
}

