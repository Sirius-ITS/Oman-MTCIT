package com.informatique.mtcit.data.model

import kotlinx.serialization.Serializable

/**
 * Request body for updating ship dimensions
 * PUT registration-requests/{requestId}/dimensions
 */
@Serializable
data class UpdateDimensionsRequest(
    val vesselLengthOverall: Double,
    val vesselBeam: Double,
    val vesselDraft: Double,
    val vesselHeight: Double,
    val decksNumber: Int
)

/**
 * Request body for updating ship weights
 * PUT registration-requests/{requestId}/weights
 */
@Serializable
data class UpdateWeightsRequest(
    val grossTonnage: Double,
    val netTonnage: Double,
    val deadweightTonnage: Double,
    val maxLoadCapacity: Double
)

