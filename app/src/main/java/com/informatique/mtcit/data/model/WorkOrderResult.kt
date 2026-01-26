package com.informatique.mtcit.data.model

import kotlinx.serialization.Serializable

/**
 * ✅ NEW: Draft Save Request (POST - first time save)
 * POST /api/v1/work-order-results
 */
@Serializable
data class DraftSaveRequest(
    val scheduledRequestId: Int,
    val answers: List<DraftAnswerSubmission>
)

/**
 * ✅ NEW: Draft Update Request (PUT - subsequent saves)
 * PUT /api/v1/work-order-results
 */
@Serializable
data class DraftUpdateRequest(
    val id: Int,  // Work order result ID from first POST
    val scheduledRequestId: Int,
    val answers: List<DraftAnswerUpdateSubmission>
)

/**
 * ✅ NEW: Answer submission for draft save (POST)
 */
@Serializable
data class DraftAnswerSubmission(
    val answer: String,
    val checklistSettingsItemId: Int
)

/**
 * ✅ NEW: Answer submission for draft update (PUT) - includes answer ID
 */
@Serializable
data class DraftAnswerUpdateSubmission(
    val id: Int? = null,  // Answer ID from previous save (null for new answers)
    val answer: String,
    val checklistSettingsItemId: Int
)

/**
 * ✅ NEW: Execute Inspection Request
 * POST /api/v1/work-order-results/execute/{id}
 *
 * Note: Only include refuseNotes for decision 2 (Refused), only include expiredDate for decision 1 (Approved)
 */
@Serializable
data class ExecuteInspectionRequest(
    val decisionId: Int,
    @kotlinx.serialization.Transient
    val refuseNotes: String? = null,  // Only serialized for decision 2
    @kotlinx.serialization.Transient
    val expiredDate: String? = null   // Only serialized for decision 1
) {
    // Custom serialization to conditionally include fields
    companion object {
        fun create(decisionId: Int, refuseNotes: String? = null, expiredDate: String? = null): Map<String, Any> {
            val map = mutableMapOf<String, Any>("decisionId" to decisionId)
            when (decisionId) {
                1 -> expiredDate?.let { map["expiredDate"] = it }
                2 -> refuseNotes?.takeIf { it.isNotBlank() }?.let { map["refuseNotes"] = it }
            }
            return map
        }
    }
}

/**
 * Work Order Result Submission Request (OLD - kept for reference)
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

