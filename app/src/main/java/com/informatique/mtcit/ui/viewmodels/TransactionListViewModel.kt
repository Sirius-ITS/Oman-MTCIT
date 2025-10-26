package com.informatique.mtcit.ui.viewmodels

import com.informatique.mtcit.ui.models.MainCategory
import com.informatique.mtcit.ui.models.Requirement
import com.informatique.mtcit.ui.models.SubCategory
import com.informatique.mtcit.ui.models.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * TransactionListViewModel - Strategy pattern implementation
 * Manages transaction list for a selected sub-category
 * Uses CategoriesRepository to avoid redundant data loading
 */
@HiltViewModel
class TransactionListViewModel @Inject constructor(
) : BaseViewModel() {

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _subCategory = MutableStateFlow<SubCategory?>(null)
    val subCategory: StateFlow<SubCategory?> = _subCategory.asStateFlow()

    private val _mainCategory = MutableStateFlow<MainCategory?>(null)
    val mainCategory: StateFlow<MainCategory?> = _mainCategory.asStateFlow()

    private val _selectedTransactions = MutableStateFlow<Set<String>>(emptySet())
    val selectedTransactions: StateFlow<Set<String>> = _selectedTransactions.asStateFlow()

    private val _requirements = MutableStateFlow<Requirement>(Requirement())
    val requirements: StateFlow<Requirement> = _requirements.asStateFlow()

    /**
     * Strategy: Load transactions for a sub-category from repository cache
     */
    fun loadTransactions(categories: List<MainCategory>, categoryId: String, subCategoryId: String) {
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

    fun getTransactionRequirements(transactionId: String) {
        val transaction = _transactions.value.find { it.id == transactionId }
        _requirements.value = transaction?.requirements!!
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
