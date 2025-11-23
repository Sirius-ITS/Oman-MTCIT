package com.informatique.mtcit.data.model.category

import kotlinx.serialization.Serializable

@Serializable
data class Step(
    val id: Int,
    val stepNameAr: String,
    val stepNameEn: String,
    val stepOrder: Int
)