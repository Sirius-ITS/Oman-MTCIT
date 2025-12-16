package com.informatique.mtcit.data.model.cancelRegistration

import kotlinx.serialization.Serializable

@Serializable
data class DeletionReasonResponse(
	val message: String? = null,
	val statusCode: Int? = null,
	val success: Boolean? = null,
	val timestamp: String? = null,
	val data: DeletionReasonData? = null,
	val errors: Map<String, String>? = null
)

@Serializable
data class DeletionReasonData(
	val number: Int? = null,
	val last: Boolean? = null,
	val size: Int? = null,
	val numberOfElements: Int? = null,
	val totalPages: Int? = null,
	val pageable: Pageable? = null,
	val sort: List<String?>? = null,
	val content: List<DeletionReasonItem?>? = null,
	val first: Boolean? = null,
	val totalElements: Int? = null,
	val empty: Boolean? = null
)

@Serializable
data class Pageable(
	val paged: Boolean? = null,
	val pageNumber: Int? = null,
	val offset: Int? = null,
	val pageSize: Int? = null,
	val unpaged: Boolean? = null,
	val sort: List<String?>? = null
)

@Serializable
data class DeletionReasonItem(
	val id: Int? = null,
	val nameAr: String? = null,
	val nameEn: String? = null,
	val isActive: Int? = null
)