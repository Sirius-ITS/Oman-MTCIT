package com.informatique.mtcit.data.model

import kotlinx.serialization.Serializable

/**
 * Inspection Decision Response
 * API: /api/v1/work-order-results/decisions-ddl
 */
@Serializable
data class InspectionDecisionResponse(
    val message: String,
    val statusCode: Int,
    val success: Boolean,
    val timestamp: String,
    val data: List<InspectionDecision>
)

/**
 * Single inspection decision option
 */
@Serializable
data class InspectionDecision(
    val id: Int,
    val nameAr: String,
    val nameEn: String,
    val name: String
)

