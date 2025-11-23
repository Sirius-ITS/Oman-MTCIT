package com.informatique.mtcit.data.model.category

import kotlinx.serialization.Serializable

@Serializable
data class TransactionDetail(
    val dateOfPublish: String? = null,
    val descAr: String,
    val descEn: String,
    val duration: Int? = null,
    val fees: Int? = null,
    val id: Int,
    val nameAr: String,
    val nameEn: String,
    val serviceOrder: Int? = null,
    val steps: List<Step>,
    val terms: List<Term>
)