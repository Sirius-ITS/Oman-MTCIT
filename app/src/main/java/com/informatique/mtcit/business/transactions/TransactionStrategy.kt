package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.ui.viewmodels.StepData

/**
 * Strategy interface for different transaction types
 * Each transaction implements its own business logic while sharing common flow
 */
interface TransactionStrategy {

    /**
     * Get all steps for this transaction
     */
    fun getSteps(): List<StepData>

    /**
     * Load dynamic dropdown options from API
     * Called during transaction initialization BEFORE getSteps()
     * @return Map of fieldId to list of option values
     */
    suspend fun loadDynamicOptions(): Map<String, List<*>> {
        // Default implementation - no dynamic options
        return emptyMap()
    }

    /**
     * Validate specific step data
     * @param step Current step index
     * @param data Form data to validate
     * @return Pair of (isValid, errors map)
     */
    fun validateStep(step: Int, data: Map<String, Any>): Pair<Boolean, Map<String, String>>

    /**
     * Process and transform step data before moving to next step
     * @param step Current step index
     * @param data Form data
     */
    fun processStepData(step: Int, data: Map<String, String>): Map<String, String>

    /**
     * Submit the transaction
     * @param data Complete form data
     * @return Result of submission
     */
    suspend fun submit(data: Map<String, String>): Result<Boolean>

    /**
     * Handle dynamic field changes (e.g., owner type change)
     * @param fieldId Field that changed
     * @param value New value
     * @param formData Current form data
     * @return Updated form data
     */
    fun handleFieldChange(fieldId: String, value: String, formData: Map<String, String>): Map<String, String> {
        return formData
    }

    /**
     * Handle field focus lost events (e.g., company lookup)
     * @param fieldId Field that lost focus
     * @param value Current value
     */
    suspend fun onFieldFocusLost(fieldId: String, value: String): FieldFocusResult {
        return FieldFocusResult.NoAction
    }
}

/**
 * Result of field focus lost event
 */
sealed class FieldFocusResult {
    object NoAction : FieldFocusResult()
    data class UpdateFields(val updates: Map<String, String>) : FieldFocusResult()
    data class Error(val fieldId: String, val message: String) : FieldFocusResult()
}
