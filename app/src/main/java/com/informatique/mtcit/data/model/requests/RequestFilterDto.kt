package com.informatique.mtcit.data.model.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import android.util.Base64

/**
 * Filter DTO for filtered requests API
 * Used with: GET /api/v1/registration-request-view/customer/filtered/{customerId}?filter=BASE64_STRING
 */
@Serializable
data class RequestFilterDto(
    @SerialName("shipName")
    val shipName: String? = null,

    @SerialName("requestSerial")
    val requestSerial: String? = null,

    @SerialName("searchDate")
    val searchDate: String? = null, // Format: yyyy-MM-dd

    @SerialName("requestTypeId")
    val requestTypeId: Int? = null,

    @SerialName("statusId")
    val statusId: Int? = null,

    @SerialName("shipTypeId")
    val shipTypeId: Int? = null,

    @SerialName("requestId")
    val requestId: Int? = null,

    @SerialName("createdBy")
    val createdBy: String? = null,

    @SerialName("page")
    val page: Int = 0,

    @SerialName("size")
    val size: Int = 10,

    @SerialName("sortBy")
    val sortBy: String = "lastChange",

    @SerialName("sortDirection")
    val sortDirection: String = "DESC"
) {
    /**
     * Convert filter to Base64 encoded JSON string
     */
    fun toBase64(): String {
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            encodeDefaults = false // Don't include null values
        }
        val jsonString = json.encodeToString(serializer(), this)
        return Base64.encodeToString(jsonString.toByteArray(), Base64.NO_WRAP)
    }

    companion object {
        /**
         * Create a basic filter for pagination without any filter criteria
         */
        fun createBasicFilter(page: Int, size: Int, sortBy: String = "lastChange", sortDirection: String = "DESC"): RequestFilterDto {
            return RequestFilterDto(
                page = page,
                size = size,
                sortBy = sortBy,
                sortDirection = sortDirection
            )
        }

        /**
         * Create a filter with status ID
         */
        fun createWithStatus(statusId: Int, page: Int, size: Int, sortBy: String = "lastChange", sortDirection: String = "DESC"): RequestFilterDto {
            return RequestFilterDto(
                statusId = statusId,
                page = page,
                size = size,
                sortBy = sortBy,
                sortDirection = sortDirection
            )
        }
    }
}

