package com.informatique.mtcit.business.transactions.marineunit.rules

import com.informatique.mtcit.business.transactions.marineunit.*
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.data.repository.MarineUnitRepository

/**
 * Base class with common validation logic shared across all transaction types
 * This reduces code duplication and ensures consistent validation
 */
abstract class BaseMarineUnitRules(
    protected val marineUnitRepository: MarineUnitRepository
) : MarineUnitBusinessRules {

    /**
     * Common ownership check - verifies user owns the marine unit
     * Called by backend API: /api/marine-units/{unitId}/verify-ownership
     */
    protected suspend fun checkOwnership(
        unit: MarineUnit,
        userId: String
    ): MarineUnitValidationResult? {
        // Backend API call to verify ownership
        val isOwned = marineUnitRepository.verifyOwnership(unit.id, userId)

        if (!isOwned) {
            return MarineUnitValidationResult.Ineligible.NotOwned(unit)
        }
        return null // Eligible, continue checking
    }

    /**
     * Common registration status check
     * Backend API: /api/marine-units/{unitId}/status
     * Returns: ACTIVE, SUSPENDED, CANCELLED
     */
    protected suspend fun checkRegistrationStatus(
        unit: MarineUnit
    ): MarineUnitValidationResult? {
        val status = marineUnitRepository.getUnitStatus(unit.id)

        when (status) {
            "SUSPENDED" -> return MarineUnitValidationResult.Ineligible.SuspendedOrCancelled(
                unit, "متوقف"
            )
            "CANCELLED" -> return MarineUnitValidationResult.Ineligible.SuspendedOrCancelled(
                unit, "ملغي"
            )
        }
        return null // Eligible
    }

    /**
     * Check if unit has permanent registration (required for most transactions)
     * Backend API: /api/marine-units/{unitId}/registration-type
     * Returns: PERMANENT, TEMPORARY
     */
    protected suspend fun checkPermanentRegistration(
        unit: MarineUnit
    ): MarineUnitValidationResult? {
        val registrationType = marineUnitRepository.getRegistrationType(unit.id)

        if (registrationType == "TEMPORARY") {
            return MarineUnitValidationResult.Ineligible.TemporaryRegistration(unit)
        }
        return null
    }

    /**
     * Check if unit has been inspected
     * Backend API: /api/marine-units/{unitId}/inspection-status
     * Used by transactions that require inspection verification
     * Returns inspection status for conditional routing
     */
    protected suspend fun checkInspectionStatus(
        unit: MarineUnit
    ): com.informatique.mtcit.data.repository.InspectionStatus {
        return marineUnitRepository.getInspectionStatus(unit.id)
    }

    /**
     * Default error message implementation
     */
    override fun getErrorMessage(result: MarineUnitValidationResult): String {
        return when (result) {
            is MarineUnitValidationResult.Eligible -> ""
            is MarineUnitValidationResult.Ineligible -> result.reason
        }
    }
}
