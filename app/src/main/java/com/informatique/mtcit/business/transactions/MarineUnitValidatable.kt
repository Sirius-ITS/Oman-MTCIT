package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.business.transactions.marineunit.MarineUnitNavigationAction
import com.informatique.mtcit.business.transactions.marineunit.MarineUnitValidationResult
import com.informatique.mtcit.business.transactions.shared.MarineUnit

/**
 * Interface for transaction strategies that require marine unit validation
 * Strategies implementing this interface can validate marine units and determine navigation
 */
interface MarineUnitValidatable {

    /**
     * Validate an existing marine unit selection
     * @param unitId The ID of the selected marine unit
     * @param userId The current user's ID
     * @return ValidationResult with navigation action
     */
    suspend fun validateMarineUnitSelection(unitId: String, userId: String): ValidationResult

    /**
     * Validate a new marine unit being added during registration
     * @param newUnit The new marine unit data
     * @param userId The current user's ID
     * @return ValidationResult with navigation action
     */
    suspend fun validateNewMarineUnit(newUnit: MarineUnit, userId: String): ValidationResult
}
