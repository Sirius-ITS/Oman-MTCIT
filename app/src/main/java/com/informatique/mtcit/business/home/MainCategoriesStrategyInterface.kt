package com.informatique.mtcit.business.home

import com.informatique.mtcit.ui.models.MainCategory

/**
 * Strategy interface for Home screen operations
 * Each strategy implements specific business logic for home features
 */
interface MainCategoriesStrategyInterface {

    /**
     * Filter categories based on criteria
     * @param categories List of categories to filter
     * @param institution Selected institution filter
     * @param organization Selected organization filter
     * @return Filtered list of categories
     */
    fun filterCategories(
        categories: List<MainCategory>,
        institution: String?,
        organization: String?
    ): List<MainCategory>

    /**
     * Search categories by query
     * @param categories List of categories to search
     * @param query Search query
     * @return Filtered list matching the query
     */
    fun searchCategories(
        categories: List<MainCategory>,
        query: String
    ): List<MainCategory>
}

