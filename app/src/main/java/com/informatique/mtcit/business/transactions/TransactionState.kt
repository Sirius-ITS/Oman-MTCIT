package com.informatique.mtcit.business.transactions

/**
 * Generic state for all transaction flows
 * Renamed from ShipRegistrationState to support multiple transaction types
 */
data class TransactionState(
    val currentStep: Int = 0,
    val steps: List<com.informatique.mtcit.ui.viewmodels.StepData> = emptyList(),
    val completedSteps: Set<Int> = emptySet(),
    val formData: Map<String, String> = emptyMap(),
    val fieldErrors: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isInitialized: Boolean = false,
    val canProceedToNext: Boolean = false,
    val transactionType: TransactionType? = null,
    // ✅ NEW: For resumed transactions - locks previous steps (no back navigation)
    val isResumedTransaction: Boolean = false,
    val lockedSteps: Set<Int> = emptySet() // Steps that cannot be navigated to
)
