package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.business.transactions.shared.StepType
import com.informatique.mtcit.ui.viewmodels.StepData

/**
 * Base Transaction Strategy - Abstract base class for all transaction strategies
 *
 * Provides common functionality:
 * ✅ Draft tracking - knows which steps were already posted
 * ✅ Data change detection - compares current data vs last posted snapshot
 * ✅ Smart POST/PUT logic - skip API calls if data unchanged
 *
 * All concrete strategies (TemporaryRegistrationStrategy, NavigationPermitStrategy, etc.)
 * should extend this class to get draft support automatically.
 *
 * @since 2026-01-12
 */
abstract class BaseTransactionStrategy : TransactionStrategy {

    // ═══════════════════════════════════════════════════════════════
    // DRAFT TRACKING STATE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Set of step types that have already been posted to the API
     * Used to determine if we should POST (first time) or PUT (update)
     */
    protected val postedSteps = mutableSetOf<StepType>()

    /**
     * Snapshot of data sent for each step
     * Key: StepType
     * Value: Map of field data that was posted
     *
     * Used to detect if data has changed since last POST
     */
    protected val stepDataSnapshots = mutableMapOf<StepType, Map<String, Any?>>()

    // ═══════════════════════════════════════════════════════════════
    // DRAFT INITIALIZATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Initialize posted steps from completed step types
     * Called when resuming a draft request
     *
     * @param completedStepTypes Set of StepType that were already completed
     */
    fun initializePostedSteps(completedStepTypes: Set<StepType>) {
        println("🔄 BaseTransactionStrategy: Initializing posted steps")
        println("   Completed steps: $completedStepTypes")

        postedSteps.clear()
        postedSteps.addAll(completedStepTypes)

        println("✅ Posted steps initialized: $postedSteps")
    }

    /**
     * Extract completed step types from API response
     * Each strategy must implement this based on its response structure
     *
     * Example for Registration:
     * ```kotlin
     * override fun extractCompletedStepsFromApiResponse(response: Any): Set<StepType> {
     *     return when (response) {
     *         is RegistrationRequestDetail -> {
     *             val steps = mutableSetOf<StepType>()
     *             if (response.shipInfo?.ship?.callSign != null) {
     *                 steps.add(StepType.MARINE_UNIT_DATA)
     *             }
     *             if (response.shipInfo?.ship?.vesselLengthOverall != null) {
     *                 steps.add(StepType.SHIP_DIMENSIONS)
     *             }
     *             // ... check other fields
     *             steps
     *         }
     *         else -> emptySet()
     *     }
     * }
     * ```
     *
     * @param response The API response object (type varies by transaction)
     * @return Set of StepType that have data in the response
     */
    abstract fun extractCompletedStepsFromApiResponse(response: Any): Set<StepType>

    // ═══════════════════════════════════════════════════════════════
    // DATA CHANGE DETECTION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Check if data for a step has changed since last POST
     *
     * @param stepType The step type to check
     * @param currentData Current form data for this step
     * @return true if data changed or no snapshot exists, false if unchanged
     */
    protected fun hasDataChanged(stepType: StepType, currentData: Map<String, Any?>): Boolean {
        val snapshot = stepDataSnapshots[stepType]

        if (snapshot == null) {
            println("🔍 No snapshot for $stepType - treating as changed")
            return true
        }

        // Compare current data with snapshot
        val changed = currentData != snapshot

        if (changed) {
            println("🔄 Data changed for $stepType")
            println("   Old: $snapshot")
            println("   New: $currentData")
        } else {
            println("⏭️ Data unchanged for $stepType")
        }

        return changed
    }

    /**
     * Save a snapshot of data after successful POST
     *
     * @param stepType The step type that was posted
     * @param data The data that was posted
     */
    protected fun saveDataSnapshot(stepType: StepType, data: Map<String, Any?>) {
        stepDataSnapshots[stepType] = data.toMap() // Make defensive copy
        println("💾 Saved snapshot for $stepType")
    }

    /**
     * Mark a step as posted
     *
     * @param stepType The step type that was posted
     */
    protected fun markStepAsPosted(stepType: StepType) {
        postedSteps.add(stepType)
        println("✅ Marked $stepType as posted")
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS FOR STRATEGIES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Check if a step has already been posted
     *
     * @param stepType The step type to check
     * @return true if this step was already posted
     */
    protected fun isStepPosted(stepType: StepType): Boolean {
        return postedSteps.contains(stepType)
    }

    /**
     * Clear all draft tracking state
     * Used when starting a new transaction (not resuming draft)
     */
    fun clearDraftState() {
        postedSteps.clear()
        stepDataSnapshots.clear()
        println("🗑️ Draft state cleared")
    }

    // ═══════════════════════════════════════════════════════════════
    // DEFAULT IMPLEMENTATIONS (Can be overridden)
    // ═══════════════════════════════════════════════════════════════

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        return emptyMap()
    }

    override fun handleFieldChange(fieldId: String, value: String, formData: Map<String, String>): Map<String, String> {
        return formData
    }

    override suspend fun onFieldFocusLost(fieldId: String, value: String): FieldFocusResult {
        return FieldFocusResult.NoAction
    }

    override suspend fun loadShipsForSelectedType(formData: Map<String, String>): List<com.informatique.mtcit.business.transactions.shared.MarineUnit> {
        return emptyList()
    }

    override suspend fun clearLoadedShips() {
        // Default: do nothing
    }

    override fun updateAccumulatedData(data: Map<String, String>) {
        // Default: do nothing
    }

    override suspend fun onStepOpened(stepIndex: Int) {
        // Default: do nothing
    }

    override fun getFormData(): Map<String, String> {
        return emptyMap()
    }

    override var onStepsNeedRebuild: (() -> Unit)? = null

    override var onLookupStarted: ((lookupKey: String) -> Unit)? = null

    override var onLookupCompleted: ((lookupKey: String, data: List<String>, success: Boolean) -> Unit)? = null

    override fun getStatusUpdateEndpoint(requestId: Int): String? {
        return null
    }

    override fun getSendRequestEndpoint(requestId: Int): String? {
        return null
    }

    override fun getCreatedRequestId(): Int? {
        return null
    }

    override fun getTransactionTypeName(): String {
        return "Transaction"
    }

    override fun storeApiResponse(apiName: String, response: Any) {
        // Default: do nothing
    }

    override fun getApiResponse(apiName: String): Any? {
        return null
    }
}

