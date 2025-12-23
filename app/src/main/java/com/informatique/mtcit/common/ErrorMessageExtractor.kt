package com.informatique.mtcit.common

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Utility object for extracting clean error messages from API responses
 *
 * Automatically extracts the "message" field from JSON error responses,
 * falling back to the full error string if parsing fails.
 *
 * Usage:
 * ```
 * val errorMessage = ErrorMessageExtractor.extract(response.error)
 * ```
 */
object ErrorMessageExtractor {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Extract a clean error message from an API error response
     *
     * @param error The error object from RepoServiceState.Error
     * @return Clean error message string
     */
    fun extract(error: Any?): String {
        if (error == null) return "Unknown error"

        return try {
            val errorString = error.toString()

            // Try to parse as JSON and extract the "message" field
            val errorElement = json.parseToJsonElement(errorString)

            if (errorElement is JsonObject) {
                // Extract message field if it exists
                errorElement.jsonObject["message"]?.jsonPrimitive?.content
                    ?: errorString
            } else {
                errorString
            }
        } catch (e: Exception) {
            // If JSON parsing fails, return the original error message
            error.toString()
        }
    }
}

