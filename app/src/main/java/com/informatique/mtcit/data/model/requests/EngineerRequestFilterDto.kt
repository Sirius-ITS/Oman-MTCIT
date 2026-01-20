package com.informatique.mtcit.data.model.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import android.util.Base64

/**
 * Simplified Filter DTO for engineer inspection requests API
 * Used with: GET /api/v1/inspection-requests/filtered/engineer?filter=BASE64_STRING
 *
 * This matches the simple filter structure: {"searchText":"","columnName":"requestNumber","page":0,"size":5}
 */
@Serializable
data class EngineerRequestFilterDto(
    @SerialName("searchText")
    val searchText: String = "",

    @SerialName("columnName")
    val columnName: String = "requestNumber",

    @SerialName("page")
    val page: Int = 0,

    @SerialName("size")
    val size: Int = 10
) {
    /**
     * Convert filter to Base64 encoded JSON string
     */
    fun toBase64(): String {
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            encodeDefaults = true // Include all values even if empty
        }
        val jsonString = json.encodeToString(serializer(), this)
        return Base64.encodeToString(
            jsonString.toByteArray(),
            Base64.NO_WRAP or Base64.URL_SAFE // Use URL_SAFE for URL encoding
        )
    }

    companion object {
        /**
         * Create a basic filter for pagination
         */
        fun create(
            searchText: String = "",
            columnName: String = "requestNumber",
            page: Int = 0,
            size: Int = 10
        ): EngineerRequestFilterDto {
            return EngineerRequestFilterDto(
                searchText = searchText,
                columnName = columnName,
                page = page,
                size = size
            )
        }
    }
}

