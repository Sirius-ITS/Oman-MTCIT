package com.informatique.mtcit.business.transactions.marineunit

import com.informatique.mtcit.business.transactions.shared.MarineUnit

/**
 * Interface for defining business rules for marine unit selection
 * in different transaction types
 */
interface MarineUnitBusinessRules {
    /**
     * Validate if marine unit is eligible for this transaction
     * This will call backend APIs to check eligibility
     */
    suspend fun validateUnit(unit: MarineUnit, userId: String): MarineUnitValidationResult

    /**
     * Get next navigation action based on validation result
     */
    fun getNavigationAction(result: MarineUnitValidationResult): MarineUnitNavigationAction

    /**
     * Get user-friendly error messages
     */
    fun getErrorMessage(result: MarineUnitValidationResult): String

    /**
     * Check if multiple units can be selected (default: false for mortgage)
     */
    fun allowMultipleSelection(): Boolean = false

    /**
     * Get transaction-specific title for marine unit selection step
     */
    fun getStepTitle(): String = "اختيار الوحدة البحرية"

    /**
     * Get transaction-specific description
     */
    fun getStepDescription(): String = "اختر الوحدة البحرية لإتمام المعاملة"
}

