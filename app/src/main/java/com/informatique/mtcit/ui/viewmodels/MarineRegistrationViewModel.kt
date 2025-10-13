package com.informatique.mtcit.ui.viewmodels

import com.informatique.mtcit.business.transactions.TransactionStrategy
import com.informatique.mtcit.business.transactions.TransactionStrategyFactory
import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.business.usecases.StepNavigationUseCase
import com.informatique.mtcit.common.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Marine Unit Registration ViewModel
 *
 * Handles Marine Unit Registration Category (التسجيل):
 * - Temporary Registration Certificate
 * - Permanent Registration Certificate
 * - Suspend Permanent Registration
 * - Cancel Permanent Registration
 * - Mortgage Certificate
 * - Release Mortgage
 */
@HiltViewModel
class MarineRegistrationViewModel @Inject constructor(
    resourceProvider: ResourceProvider,
    navigationUseCase: StepNavigationUseCase,
    private val strategyFactory: TransactionStrategyFactory
) : BaseTransactionViewModel(resourceProvider, navigationUseCase) {

    /**
     * Create strategy for Marine Unit Registration transactions
     */
    override suspend fun createStrategy(transactionType: TransactionType): TransactionStrategy {
        // Validate that this is a marine registration transaction
        require(isMarineRegistrationTransaction(transactionType)) {
            "MarineRegistrationViewModel can only handle marine registration transactions, got: $transactionType"
        }

        // Delegate to factory to create the appropriate strategy
        return strategyFactory.create(transactionType)
    }

    /**
     * Check if transaction type belongs to Marine Unit Registration category
     */
    private fun isMarineRegistrationTransaction(type: TransactionType): Boolean {
        return when (type) {
            TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE,
            TransactionType.PERMANENT_REGISTRATION_CERTIFICATE,
            TransactionType.SUSPEND_PERMANENT_REGISTRATION,
            TransactionType.CANCEL_PERMANENT_REGISTRATION,
            TransactionType.MORTGAGE_CERTIFICATE,
            TransactionType.RELEASE_MORTGAGE -> true
            else -> false
        }
    }
}

