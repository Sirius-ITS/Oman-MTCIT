package com.informatique.mtcit.business.transactions.marineunit.usecases

import com.informatique.mtcit.business.transactions.marineunit.*
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.data.repository.MarineUnitRepository
import javax.inject.Inject

/**
 * Use case to filter marine units and return only those eligible for the transaction
 * This is useful for pre-filtering the list before showing to user
 */
class GetEligibleMarineUnitsUseCase @Inject constructor(
    private val marineUnitRepository: MarineUnitRepository,
    private val validateUseCase: ValidateMarineUnitUseCase
) {

    /**
     * Get all units with their validation results
     * Useful for showing both eligible and ineligible units with reasons
     *
     * @param userId The current user ID
     * @param rules Transaction-specific business rules
     * @return List of pairs (MarineUnit, ValidationResult)
     */
    suspend fun execute(
        userId: String,
        rules: MarineUnitBusinessRules
    ): List<Pair<MarineUnit, MarineUnitValidationResult>> {
        val allUnits = marineUnitRepository.getUserMarineUnits(userId)

        return allUnits.map { unit ->
            val result = validateUseCase.execute(unit, userId, rules)
            Pair(unit, result)
        }
    }

    /**
     * Get only eligible units (filters out ineligible ones)
     *
     * @param userId The current user ID
     * @param rules Transaction-specific business rules
     * @return List of eligible marine units only
     */
    suspend fun getEligibleOnly(
        userId: String,
        rules: MarineUnitBusinessRules
    ): List<MarineUnit> {
        return execute(userId, rules)
            .filter { it.second is MarineUnitValidationResult.Eligible }
            .map { it.first }
    }

    /**
     * Get units grouped by eligibility status
     *
     * @param userId The current user ID
     * @param rules Transaction-specific business rules
     * @return Pair of (eligibleUnits, ineligibleUnitsWithReasons)
     */
    suspend fun getGroupedByEligibility(
        userId: String,
        rules: MarineUnitBusinessRules
    ): Pair<List<MarineUnit>, List<Pair<MarineUnit, String>>> {
        val allResults = execute(userId, rules)

        val eligible = allResults
            .filter { it.second is MarineUnitValidationResult.Eligible }
            .map { it.first }

        val ineligible = allResults
            .filter { it.second is MarineUnitValidationResult.Ineligible }
            .map {
                val result = it.second as MarineUnitValidationResult.Ineligible
                Pair(it.first, result.reason)
            }

        return Pair(eligible, ineligible)
    }
}

