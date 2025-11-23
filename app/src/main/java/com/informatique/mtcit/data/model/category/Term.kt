package com.informatique.mtcit.data.model.category

import kotlinx.serialization.Serializable

@Serializable
data class Term(
    val id: Int,
    val nameAr: String,
    val nameEn: String
)