package com.informatique.mtcit.ui.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.informatique.mtcit.R
import com.informatique.mtcit.business.home.MainCategoriesStrategyInterface
import com.informatique.mtcit.business.home.MainCategoriesStrategyFactory
import com.informatique.mtcit.ui.models.MainCategory
import com.informatique.mtcit.data.model.category.SubCategory
import com.informatique.mtcit.data.model.category.TransactionDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @param:ApplicationContext val context: Context,
    val strategyFactory: MainCategoriesStrategyFactory
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

    private val _requirementsTabList = MutableStateFlow(listOf<String>().toMutableList())
    val requirementsTabList: StateFlow<MutableList<String>> = _requirementsTabList.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _subCategories = MutableStateFlow<SubCategoriesUiState>(SubCategoriesUiState.Blank)
    val subCategories: StateFlow<SubCategoriesUiState> = _subCategories.asStateFlow()

    private val _transactionDetail = MutableStateFlow<TransactionDetailUiState>(TransactionDetailUiState.Blank)
    val transactionDetail: StateFlow<TransactionDetailUiState> = _transactionDetail.asStateFlow()

    init {
        // Initialize strategy only
        currentStrategy = strategyFactory.createCategoriesStrategy()

        getSubCategoriesApi()
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

    fun getSubCategoriesApi(){
        viewModelScope.launch {
            _subCategories.value = SubCategoriesUiState.Loading

            strategyFactory.fetchSubCategories()
                .onSuccess {
                    _subCategories.value = SubCategoriesUiState.Success(it)
                }
                .onFailure { error ->
                    _subCategories.value = SubCategoriesUiState.Error(
                        error.message ?: "Unknown error"
                    )
                }
        }
    }

    fun getTransactionDetailApi(serviceId: Int){
        viewModelScope.launch {
            _transactionDetail.value = TransactionDetailUiState.Loading

            strategyFactory.fetchTransactionDetail(serviceId = serviceId)
                .onSuccess {
                    _transactionDetail.value = TransactionDetailUiState.Success(it)

                    if (_transactionDetail.value is TransactionDetailUiState.Success){
                        val requirements = (_transactionDetail.value as TransactionDetailUiState.Success).detail
                        if (requirements.fees != null){
                            _requirementsTabList.value.add(context.getString(R.string.requirements_fees_title))
                        }
                        if (requirements.steps.isNotEmpty()){
                            _requirementsTabList.value.add(context.getString(R.string.requirements_steps_title))
                        }
                        if (requirements.terms.isNotEmpty()){
                            _requirementsTabList.value.add(context.getString(R.string.requirements_terms_title))
                        }
                    }
                }
                .onFailure { error ->
                    _transactionDetail.value = TransactionDetailUiState.Error(
                        error.message ?: "Unknown error"
                    )
                }
        }
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
    fun getSubCategories(categoryId: String): List<com.informatique.mtcit.ui.models.SubCategory> {
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

sealed interface SubCategoriesUiState {
    data object Blank: SubCategoriesUiState
    data object Loading: SubCategoriesUiState
    data class Success(val subcategories: List<SubCategory>) : SubCategoriesUiState
    data class Error(val message: String) : SubCategoriesUiState
}

sealed interface TransactionDetailUiState {
    data object Blank: TransactionDetailUiState
    data object Loading: TransactionDetailUiState
    data class Success(val detail: TransactionDetail) : TransactionDetailUiState
    data class Error(val message: String) : TransactionDetailUiState
}
