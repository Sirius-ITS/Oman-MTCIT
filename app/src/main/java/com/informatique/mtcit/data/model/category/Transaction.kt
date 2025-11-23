package com.informatique.mtcit.data.model.category

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Transaction(
    val id: Int,
    val nameAr: String,
    val nameEn: String,
    @Transient val serviceOrder: Int? = null
)