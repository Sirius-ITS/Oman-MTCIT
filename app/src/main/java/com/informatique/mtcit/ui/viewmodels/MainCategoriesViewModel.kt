package com.informatique.mtcit.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.informatique.mtcit.business.home.MainCategoriesStrategyInterface
import com.informatique.mtcit.business.home.MainCategoriesStrategyFactory
import com.informatique.mtcit.ui.models.MainCategory
import com.informatique.mtcit.ui.models.SubCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MainCategoriesViewModel - Manages UI state for MainCategoriesScreen
 * Responsibilities: Filtering, Expansion, Search - NOT loading categories
 * Categories are provided globally via CompositionLocal from LandingViewModel
 */
@HiltViewModel
class MainCategoriesViewModel @Inject constructor(
    strategyFactory: MainCategoriesStrategyFactory
) : BaseViewModel() {

    // Current strategy for categories management
    private var currentStrategy: MainCategoriesStrategyInterface? = null

    // Categories received from CompositionLocal
    private val _categories = MutableStateFlow<List<MainCategory>>(emptyList())

    private val _filteredCategories = MutableStateFlow<List<MainCategory>>(emptyList())
    val filteredCategories: StateFlow<List<MainCategory>> = _filteredCategories.asStateFlow()

    private val _expandedCategories = MutableStateFlow<Set<String>>(emptySet())
    val expandedCategories: StateFlow<Set<String>> = _expandedCategories.asStateFlow()

    private val _selectedInstitution = MutableStateFlow<String?>(null)
    val selectedInstitution: StateFlow<String?> = _selectedInstitution.asStateFlow()

    private val _selectedOrganization = MutableStateFlow<String?>(null)
    val selectedOrganization: StateFlow<String?> = _selectedOrganization.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        // Initialize strategy only
        currentStrategy = strategyFactory.createCategoriesStrategy()
    }

    /**
     * Set categories from external source (CompositionLocal)
     * This should be called when categories are available from CompositionLocal
     */
    fun setCategories(categories: List<MainCategory>) {
        _categories.value = categories
        applyFilters()
        setLoading(false) // Ensure loading state is false when categories are set
    }

    /**
     * Search categories using strategy
     */
    fun searchCategories(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    /**
     * Apply filters using strategy
     */
    private fun applyFilters() {
        viewModelScope.launch {
            currentStrategy?.let { strategy ->
                var filtered = _categories.value

                // Apply search filter
                if (_searchQuery.value.isNotBlank()) {
                    filtered = strategy.searchCategories(filtered, _searchQuery.value)
                }

                // Apply institution/organization filters
                filtered = strategy.filterCategories(
                    filtered,
                    _selectedInstitution.value,
                    _selectedOrganization.value
                )

                _filteredCategories.value = filtered
            }
        }
    }

    /**
     * Toggle category expansion
     */
    fun toggleCategoryExpansion(categoryId: String) {
        val currentExpanded = _expandedCategories.value.toMutableSet()
        if (currentExpanded.contains(categoryId)) {
            currentExpanded.remove(categoryId)
        } else {
            currentExpanded.add(categoryId)
        }
        _expandedCategories.value = currentExpanded
    }

    /**
     * Expand a specific category (used when navigating from home screen)
     */
    fun expandCategory(categoryId: String) {
        val currentExpanded = _expandedCategories.value.toMutableSet()
        currentExpanded.add(categoryId)
        _expandedCategories.value = currentExpanded
    }

    /**
     * Check if category is expanded
     */
    fun isCategoryExpanded(categoryId: String): Boolean {
        return _expandedCategories.value.contains(categoryId)
    }

    /**
     * Get subcategories for a category
     */
    fun getSubCategories(categoryId: String): List<SubCategory> {
        return _filteredCategories.value.find { it.id == categoryId }?.subCategories ?: emptyList()
    }

    /**
     * Update selected institution filter
     */
    fun selectInstitution(institution: String?) {
        _selectedInstitution.value = institution
        applyFilters()
    }

    /**
     * Update selected organization filter
     */
    fun selectOrganization(organization: String?) {
        _selectedOrganization.value = organization
        applyFilters()
    }

    /**
     * Get available services count for a category
     */
    fun getAvailableServicesCount(categoryId: String): Int {
        val category = _categories.value.find { it.id == categoryId }
        return category?.subCategories?.sumOf { it.transactions.size } ?: 0
    }
}
