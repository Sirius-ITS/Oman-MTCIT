package com.informatique.mtcit.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.informatique.mtcit.business.transactions.LoginStrategy
import com.informatique.mtcit.business.transactions.TransactionStrategy
import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.business.usecases.StepNavigationUseCase
import com.informatique.mtcit.common.ResourceProvider
import com.informatique.mtcit.ui.base.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Login/Registration ViewModel
 *
 * Handles user authentication flow before accessing transactions
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    resourceProvider: ResourceProvider,
    navigationUseCase: StepNavigationUseCase,
    private val loginStrategy: LoginStrategy
) : BaseTransactionViewModel(resourceProvider, navigationUseCase) {

    // Event to notify when login is complete
    private val _loginComplete = MutableSharedFlow<Boolean>()
    val loginComplete = _loginComplete.asSharedFlow()

    init {
        // Initialize login flow with a dummy transaction type
        initializeTransaction(TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE)
    }

    /**
     * Create strategy - returns LoginStrategy
     */
    override suspend fun createStrategy(transactionType: TransactionType): TransactionStrategy {
        return loginStrategy
    }

    /**
     * Monitor submission state to detect login completion
     */
    init {
        viewModelScope.launch {
            submissionState.collect { state ->
                if (state is UIState.Success) {
                    println("✅ Login successful! Emitting completion event...")
                    _loginComplete.emit(true)
                }
            }
        }
    }
}

