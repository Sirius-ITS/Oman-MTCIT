package com.informatique.mtcit.data.model.cancelRegistration

import kotlinx.serialization.Serializable

@Serializable
data class DeletionSubmitResponse(
    val message: String? = null,
    val statusCode: Int? = null,
    val success: Boolean? = null,
    val timestamp: String? = null,
    val data: DeletionRequestData? = null,
    val errors: Map<String, String>? = null // لو فيه أخطاء في الفيلدز
)

@Serializable
data class DeletionRequestData(
    val id: Int,
    val deletionReasonId: Int,
    val shipInfoId: Int,
    val statusId: Int,
    val createdAt: String?,
    val updatedAt: String?
)