package com.informatique.mtcit.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.informatique.mtcit.business.home.HomeStrategy
import com.informatique.mtcit.business.home.HomeStrategyFactory
import com.informatique.mtcit.data.repository.CategoriesRepositoryImpl
import com.informatique.mtcit.ui.models.MainCategory
import com.informatique.mtcit.ui.models.SubCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HomeViewModel - Strategy pattern implementation
 * Manages categories and sub-categories for the home screen
 * Uses Strategy pattern similar to transactions for business logic separation
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val strategyFactory: HomeStrategyFactory,
    private val repository: CategoriesRepositoryImpl
) : BaseViewModel() {

    // Current strategy for categories management
    private var currentStrategy: HomeStrategy? = null

    private val _categories = MutableStateFlow<List<MainCategory>>(emptyList())
    val categories: StateFlow<List<MainCategory>> = _categories.asStateFlow()

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
        // Initialize strategy
        currentStrategy = strategyFactory.createCategoriesStrategy()
        loadCategories()
    }

    /**
     * Strategy: Load categories from repository via strategy
     */
    fun loadCategories() {
        viewModelScope.launch {
            setLoading(true)
            try {
                currentStrategy?.let { strategy ->
                    val result = strategy.loadCategories()
                    result.fold(
                        onSuccess = { categoriesData ->
                            _categories.value = categoriesData
                            applyFilters()
                            setLoading(false)
                        },
                        onFailure = { error ->
                            setError(error.message ?: "Failed to load categories")
                            setLoading(false)
                        }
                    )
                }
            } catch (e: Exception) {
                setError(e.message ?: "Failed to load categories")
                setLoading(false)
            }
        }
    }

    /**
     * Refresh categories - clears cache and reloads
     */
    fun refreshCategories() {
        repository.clearCache()
        loadCategories()
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
        val category = _filteredCategories.value.find { it.id == categoryId }
        return category?.subCategories?.sumOf { it.transactions.size } ?: 0
    }
}
