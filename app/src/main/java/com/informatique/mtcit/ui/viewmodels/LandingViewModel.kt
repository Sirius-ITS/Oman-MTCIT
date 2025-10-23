package com.informatique.mtcit.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.informatique.mtcit.business.landing.LandingStrategyFactory
import com.informatique.mtcit.business.landing.LandingStrategyInterface
import com.informatique.mtcit.ui.models.MainCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * LandingViewModel - Responsible ONLY for initial app setup and loading categories
 * Categories are then provided globally via CompositionLocal
 */
@HiltViewModel
class LandingViewModel @Inject constructor(
    private val strategyFactory: LandingStrategyFactory
) : BaseViewModel() {

    private var currentStrategy: LandingStrategyInterface? = null

    private val _categories = MutableStateFlow<List<MainCategory>>(emptyList())
    val categories: StateFlow<List<MainCategory>> = _categories.asStateFlow()

    init {
        currentStrategy = strategyFactory.createLandingStrategy()
        loadCategories()
    }

    /**
     * Load categories using Landing Strategy
     * This is called once when the app starts
     */
    private fun loadCategories() {
        viewModelScope.launch {
            setLoading(true)
            try {
                currentStrategy?.let { strategy ->
                    val result = strategy.loadCategories()
                    result.fold(
                        onSuccess = { categoriesData ->
                            _categories.value = categoriesData
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
}

