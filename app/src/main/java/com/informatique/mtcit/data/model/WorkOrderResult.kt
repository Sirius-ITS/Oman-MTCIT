package com.informatique.mtcit.data.model

import kotlinx.serialization.Serializable

/**
 * Work Order Result Submission Request
 * POST /api/v1/work-order-results
 */
@Serializable
data class WorkOrderResultRequest(
    val decisionId: Int,
    val answers: List<WorkOrderAnswerSubmission>,
    val scheduledRequestId: Int,
    val expiredDate: String? = null  // Only required if decisionId = 1 (Accepted)
)

/**
 * Checklist answer in work order result submission
 * Different from ChecklistAnswer which is used for display
 */
@Serializable
data class WorkOrderAnswerSubmission(
    val answer: String,  // Can be choice ID or text
    val checklistSettingsItemId: Int
)

/**
 * Work Order Result Submission Response
 * Note: API returns data as Int (the created work order result ID), not as an object
 */
@Serializable
data class WorkOrderResultResponse(
    val message: String,
    val statusCode: Int,
    val success: Boolean,
    val timestamp: String,
    val data: Int? = null  // ✅ Changed from WorkOrderResultData to Int (the ID of created work order result)
)

