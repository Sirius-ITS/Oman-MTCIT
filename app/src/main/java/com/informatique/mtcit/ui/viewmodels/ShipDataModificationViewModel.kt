package com.informatique.mtcit.ui.viewmodels

import com.informatique.mtcit.business.transactions.TransactionStrategy
import com.informatique.mtcit.business.transactions.TransactionStrategyFactory
import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.business.usecases.StepNavigationUseCase
import com.informatique.mtcit.common.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Ship Data Modification ViewModel
 *
 * Handles Ship Data Modifications Category (تعديل بيانات السفينة):
 * - Ship Registration
 * - Ship Name Change
 * - Ship Dimensions Change
 * - Captain Name Change
 * - Ship Activity Change
 * - Ship Engine Change
 * - Ship Port Change
 * - Ship Ownership Change
 * - etc.
 */
@HiltViewModel
class ShipDataModificationViewModel @Inject constructor(
    resourceProvider: ResourceProvider,
    navigationUseCase: StepNavigationUseCase,
    private val strategyFactory: TransactionStrategyFactory
) : BaseTransactionViewModel(resourceProvider, navigationUseCase) {

    /**
     * Create strategy for Ship Data Modification transactions
     */
    override suspend fun createStrategy(transactionType: TransactionType): TransactionStrategy {
        // Validate that this is a ship data modification transaction
        require(isShipDataModificationTransaction(transactionType)) {
            "ShipDataModificationViewModel can only handle ship data modification transactions, got: $transactionType"
        }

        // Delegate to factory to create the appropriate strategy
        return strategyFactory.create(transactionType)
    }

    /**
     * Check if transaction type belongs to Ship Data Modifications category
     */
    private fun isShipDataModificationTransaction(type: TransactionType): Boolean {
        return when (type) {
            TransactionType.SHIP_NAME_CHANGE,
            TransactionType.CAPTAIN_NAME_CHANGE,
            TransactionType.SHIP_ACTIVITY_CHANGE,
            TransactionType.SHIP_DIMENSIONS_CHANGE,
            TransactionType.SHIP_ENGINE_CHANGE,
            TransactionType.SHIP_PORT_CHANGE,
            TransactionType.SHIP_OWNERSHIP_CHANGE -> true
            else -> false
        }
    }
}

