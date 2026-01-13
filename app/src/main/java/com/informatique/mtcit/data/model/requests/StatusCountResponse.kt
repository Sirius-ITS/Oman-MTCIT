package com.informatique.mtcit.data.model.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response for count-by-status API endpoint
 * Endpoint: GET /registration-request-view/customer/count-by-status/{customerId}
 */
@Serializable
data class StatusCountResponse(
    @SerialName("message")
    val message: String,

    @SerialName("statusCode")
    val statusCode: Int,

    @SerialName("success")
    val success: Boolean,

    @SerialName("timestamp")
    val timestamp: String,

    @SerialName("data")
    val data: StatusCountData?
)

@Serializable
data class StatusCountData(
    @SerialName("totalCount")
    val totalCount: Int,

    @SerialName("statusCounts")
    val statusCounts: List<StatusCount>
)

@Serializable
data class StatusCount(
    @SerialName("statusId")
    val statusId: Int,

    @SerialName("count")
    val count: Int
)
