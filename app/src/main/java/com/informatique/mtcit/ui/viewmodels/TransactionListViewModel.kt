package com.informatique.mtcit.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.informatique.mtcit.data.repository.CategoriesRepository
import com.informatique.mtcit.ui.models.MainCategory
import com.informatique.mtcit.ui.models.SubCategory
import com.informatique.mtcit.ui.models.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * TransactionListViewModel - Strategy pattern implementation
 * Manages transaction list for a selected sub-category
 * Uses CategoriesRepository to avoid redundant data loading
 */
@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val repository: CategoriesRepository
) : BaseViewModel() {

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _subCategory = MutableStateFlow<SubCategory?>(null)
    val subCategory: StateFlow<SubCategory?> = _subCategory.asStateFlow()

    private val _mainCategory = MutableStateFlow<MainCategory?>(null)
    val mainCategory: StateFlow<MainCategory?> = _mainCategory.asStateFlow()

    private val _selectedTransactions = MutableStateFlow<Set<String>>(emptySet())
    val selectedTransactions: StateFlow<Set<String>> = _selectedTransactions.asStateFlow()

    /**
     * Strategy: Load transactions for a sub-category from repository cache
     */
    fun loadTransactions(categoryId: String, subCategoryId: String) {
        viewModelScope.launch {
            setLoading(true)
            try {
                // Use repository instead of calling getMainCategories() directly
                // Repository will return cached data if available
                repository.getCategories().fold(
                    onSuccess = { categories ->
                        val category = categories.find { it.id == categoryId }
                        val subCat = category?.subCategories?.find { it.id == subCategoryId }

                        if (subCat != null) {
                            _mainCategory.value = category
                            _subCategory.value = subCat
                            _transactions.value = subCat.transactions
                            setLoading(false)
                        } else {
                            setError("Sub-category not found")
                            setLoading(false)
                        }
                    },
                    onFailure = { error ->
                        setError(error.message ?: "Failed to load transactions")
                        setLoading(false)
                    }
                )
            } catch (e: Exception) {
                setError(e.message ?: "Failed to load transactions")
                setLoading(false)
            }
        }
    }

    /**
     * Toggle transaction selection
     */
    fun toggleTransactionSelection(transactionId: String) {
        val currentSelected = _selectedTransactions.value.toMutableSet()
        if (currentSelected.contains(transactionId)) {
            currentSelected.remove(transactionId)
        } else {
            currentSelected.add(transactionId)
        }
        _selectedTransactions.value = currentSelected
    }

    /**
     * Check if transaction is selected
     */
    fun isTransactionSelected(transactionId: String): Boolean {
        return _selectedTransactions.value.contains(transactionId)
    }

    /**
     * Clear all selections
     */
    fun clearSelections() {
        _selectedTransactions.value = emptySet()
    }

    /**
     * Get selected transactions count
     */
    fun getSelectedCount(): Int {
        return _selectedTransactions.value.size
    }
}
