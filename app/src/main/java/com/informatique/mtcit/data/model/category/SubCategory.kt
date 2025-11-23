package com.informatique.mtcit.data.model.category

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubCategory(
    val descAr: String? = null,
    val descEn: String? = null,
    val id: Int,
    val nameAr: String,
    val nameEn: String,
    val categoryOrder: Int? = null,
    @SerialName("services")
    val transactions: List<Transaction>
)