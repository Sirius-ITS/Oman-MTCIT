package com.informatique.mtcit.data.model.category

import kotlinx.serialization.Serializable

@Serializable
data class TransactionDetail(
    val id: Int,
    val nameAr: String,
    val nameEn: String,
    val name: String,
    val descAr: String,
    val descEn: String,
    val desc: String,
    val dateOfPublish: String? = null,
    val serviceOrder: Int? = null,
    val duration: Int? = null,
    val requestTypeId: Int,
    val hasInvoice: Int? = null,
    val hasAcceptance: Int? = null,
    val steps: List<Step>,
    val terms: List<Term>,
    val tariffItemResDtos: List<TariffItem>? = null,
    val coreDocumentsResDtos: List<CoreDocument>? = null
)

@Serializable
data class TariffItem(
    val nameAr: String,
    val nameEn: String,
    val name: String,
    val calculationType: String
)

@Serializable
data class CoreDocument(
    val id: Int,
    val nameAr: String,
    val nameEn: String,
    val name: String,
    val docOrder: Int,
    val isMandatory: Int,
    val isActive: Int
)