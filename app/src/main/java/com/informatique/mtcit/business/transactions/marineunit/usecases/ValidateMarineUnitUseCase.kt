package com.informatique.mtcit.business.transactions.marineunit.usecases

import com.informatique.mtcit.business.transactions.marineunit.*
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import javax.inject.Inject

/**
 * Use case for validating marine unit against transaction-specific business rules
 * This encapsulates the validation logic and makes it reusable across ViewModels
 */
class ValidateMarineUnitUseCase @Inject constructor() {

    /**
     * Execute validation for a single marine unit
     *
     * @param unit The marine unit to validate
     * @param userId The current user ID for ownership verification
     * @param rules Transaction-specific business rules
     * @return Validation result (Eligible or Ineligible with reason)
     */
    suspend fun execute(
        unit: MarineUnit,
        userId: String,
        rules: MarineUnitBusinessRules
    ): MarineUnitValidationResult {
        return rules.validateUnit(unit, userId)
    }

    /**
     * Execute validation and get navigation action in one call
     *
     * @param unit The marine unit to validate
     * @param userId The current user ID
     * @param rules Transaction-specific business rules
     * @return Pair of (ValidationResult, NavigationAction)
     */
    suspend fun executeAndGetAction(
        unit: MarineUnit,
        userId: String,
        rules: MarineUnitBusinessRules
    ): Pair<MarineUnitValidationResult, MarineUnitNavigationAction> {
        val result = rules.validateUnit(unit, userId)
        val action = rules.getNavigationAction(result)
        return Pair(result, action)
    }

    /**
     * Validate multiple units at once (useful for batch operations)
     *
     * @param units List of marine units to validate
     * @param userId The current user ID
     * @param rules Transaction-specific business rules
     * @return Map of unit ID to validation result
     */
    suspend fun executeForMultiple(
        units: List<MarineUnit>,
        userId: String,
        rules: MarineUnitBusinessRules
    ): Map<String, MarineUnitValidationResult> {
        return units.associate { unit ->
            unit.id to rules.validateUnit(unit, userId)
        }
    }
}

