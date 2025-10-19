package com.informatique.mtcit.business.home

import com.informatique.mtcit.data.repository.CategoriesRepository
import com.informatique.mtcit.ui.models.MainCategory
import javax.inject.Inject

/**
 * Strategy for loading and managing categories on the home screen
 * Handles category loading from repository and filtering logic
 */
class CategoriesStrategy @Inject constructor(
    private val repository: CategoriesRepository
) : HomeStrategy {

    override suspend fun loadCategories(): Result<List<MainCategory>> {
        return repository.getCategories()
    }

    override fun filterCategories(
        categories: List<MainCategory>,
        institution: String?,
        organization: String?
    ): List<MainCategory> {
        // If no filters applied, return all
        if (institution == null && organization == null) {
            return categories
        }

        // Apply filtering logic
        return categories.filter { category ->
            // Add your filtering logic here based on institution/organization
            // For now, return all (can be customized based on requirements)
            true
        }
    }

    override fun searchCategories(
        categories: List<MainCategory>,
        query: String
    ): List<MainCategory> {
        if (query.isBlank()) {
            return categories
        }

        val lowercaseQuery = query.lowercase()

        return categories.mapNotNull { category ->
            // Search in subcategories and transactions
            val matchingSubCategories = category.subCategories.mapNotNull { subCategory ->
                val matchingTransactions = subCategory.transactions.filter { transaction ->
                    // Match transaction titles (would need context to get string resource)
                    transaction.id.contains(lowercaseQuery, ignoreCase = true)
                }

                if (matchingTransactions.isNotEmpty() ||
                    subCategory.id.contains(lowercaseQuery, ignoreCase = true)) {
                    subCategory.copy(transactions = matchingTransactions.ifEmpty { subCategory.transactions })
                } else {
                    null
                }
            }

            if (matchingSubCategories.isNotEmpty() ||
                category.id.contains(lowercaseQuery, ignoreCase = true)) {
                category.copy(subCategories = matchingSubCategories.ifEmpty { category.subCategories })
            } else {
                null
            }
        }
    }
}

