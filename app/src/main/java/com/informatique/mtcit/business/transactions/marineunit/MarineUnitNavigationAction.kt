package com.informatique.mtcit.business.transactions.marineunit

import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.TransactionType

/**
 * Sealed class representing navigation actions after marine unit validation
 */
sealed class MarineUnitNavigationAction {
    /**
     * Proceed to next step in transaction flow
     */
    data class ProceedToNextStep(
        val selectedUnit: MarineUnit,
        val additionalData: Map<String, Any> = emptyMap()
    ) : MarineUnitNavigationAction()

    /**
     * Jump to specific step (skip intermediate steps)
     */
    data class JumpToStep(
        val stepIndex: Int,
        val reason: String
    ) : MarineUnitNavigationAction()

    /**
     * Show error state with message
     */
    data class ShowError(
        val title: String,
        val message: String,
        val actions: List<ErrorAction> = emptyList()
    ) : MarineUnitNavigationAction()

    /**
     * Redirect to different transaction
     */
    data class RedirectToTransaction(
        val transactionType: TransactionType,
        val reason: String,
        val prefilledData: Map<String, String> = emptyMap()
    ) : MarineUnitNavigationAction()

    /**
     * Show confirmation dialog before proceeding
     */
    data class ShowConfirmation(
        val message: String,
        val onConfirm: MarineUnitNavigationAction
    ) : MarineUnitNavigationAction()

    /**
     * NEW: Show RequestDetailScreen with full marine unit data and compliance issues
     * Used when "استدعاء بيانات السفينة ومراجعة سجل الالتزام" returns cannot proceed
     */
    data class ShowComplianceDetailScreen(
        val marineUnit: MarineUnit,
        val complianceIssues: List<ComplianceIssue>,
        val rejectionReason: String,
        val rejectionTitle: String = "تم رفض الطلب"
    ) : MarineUnitNavigationAction()

    /**
     * NEW: Route to different step based on condition (e.g., inspection status)
     * Used for dynamic step routing within the same transaction
     */
    data class RouteToConditionalStep(
        val selectedUnit: MarineUnit,
        val targetStepIndex: Int,
        val condition: String, // e.g., "INSPECTED", "NOT_INSPECTED"
        val conditionData: Map<String, Any> = emptyMap()
    ) : MarineUnitNavigationAction()
}

/**
 * Action that can be taken from an error state
 */
data class ErrorAction(
    val label: String,
    val action: MarineUnitNavigationAction
)

/**
 * NEW: Represents a compliance issue found during marine unit validation
 */
data class ComplianceIssue(
    val category: String,        // e.g., "المخالفات", "الديون", "الرهونات"
    val title: String,           // e.g., "وجود مخالفات نشطة"
    val description: String,     // Detailed description
    val severity: IssueSeverity, // Blocking, Warning, Info
    val details: Map<String, String> = emptyMap() // Additional details
)

enum class IssueSeverity {
    BLOCKING,  // Cannot proceed with transaction
    WARNING,   // Can proceed but with caution
    INFO       // Informational only
}
