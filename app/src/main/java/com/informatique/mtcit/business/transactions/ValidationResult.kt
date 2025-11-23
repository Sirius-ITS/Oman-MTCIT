package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.business.transactions.marineunit.MarineUnitNavigationAction
import com.informatique.mtcit.business.transactions.marineunit.MarineUnitValidationResult

/**
 * Result wrapper for validation operations in transaction strategies
 */
sealed class ValidationResult {
    data class Success(
        val validationResult: MarineUnitValidationResult,
        val navigationAction: MarineUnitNavigationAction
    ) : ValidationResult()

    data class Error(val message: String) : ValidationResult()
}

