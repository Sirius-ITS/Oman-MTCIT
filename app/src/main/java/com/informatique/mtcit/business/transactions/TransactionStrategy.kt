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
     * @return Required next step index (normally returns same step, or -1 to stop flow)
     */
    suspend fun processStepData(step: Int, data: Map<String, String>): Int

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

    /**
     * ✅ NEW: Load ships when user selects type and presses Next
     * Called from ViewModel when navigating from person type / commercial reg step
     */
    suspend fun loadShipsForSelectedType(formData: Map<String, String>): List<com.informatique.mtcit.business.transactions.shared.MarineUnit> {
        return emptyList() // Default: no ships
    }

    /**
     * ✅ NEW: Clear loaded ships when user goes back
     * This ensures fresh data is loaded when user changes person type
     */
    suspend fun clearLoadedShips() {
        // Default: do nothing
    }

    /**
     * ✅ NEW: Update accumulated form data immediately when field changes
     * This allows getSteps() to have latest data for dynamic step logic
     */
    fun updateAccumulatedData(data: Map<String, String>) {
        // Default: do nothing
    }

    /**
     * Called when a step is opened by the UI
     * Used for lazy loading of lookups specific to that step
     * @param stepIndex Index of the step being opened
     */
    suspend fun onStepOpened(stepIndex: Int) {
        // Default no-op implementation - strategies can override to load step-specific lookups
    }

    /**
     * ✅ NEW: Get current form data including any flags set by strategy (e.g., showInspectionDialog)
     * @return Current accumulated form data
     */
    fun getFormData(): Map<String, String> {
        // Default implementation - return empty map
        return emptyMap()
    }

    /**
     * ✅ NEW: Callback to notify ViewModel when steps need to be rebuilt
     * This is used after lazy-loading lookups to refresh the UI with updated dropdown options
     * Strategies can set this callback and invoke it when their step data changes
     */
    var onStepsNeedRebuild: (() -> Unit)?
        get() = null
        set(_) {}

    /**
     * ✅ NEW: Callback to notify ViewModel when a specific lookup starts loading
     * This enables per-field loading indicators in the UI
     * @param lookupKey The lookup identifier (e.g., "ports", "countries", "shipTypes")
     */
    var onLookupStarted: ((lookupKey: String) -> Unit)?
        get() = null
        set(_) {}

    /**
     * ✅ NEW: Callback to notify ViewModel when a specific lookup finishes loading
     * This enables per-field loading indicators in the UI
     * @param lookupKey The lookup identifier (e.g., "ports", "countries", "shipTypes")
     * @param data The loaded data for this lookup (empty list on error)
     * @param success Whether the lookup succeeded
     */
    var onLookupCompleted: ((lookupKey: String, data: List<String>, success: Boolean) -> Unit)?
        get() = null
        set(_) {}
}

/**
 * Result of field focus lost event
 */
sealed class FieldFocusResult {
    object NoAction : FieldFocusResult()
    data class UpdateFields(val updates: Map<String, String>) : FieldFocusResult()
    data class Error(val fieldId: String, val message: String) : FieldFocusResult()
}
