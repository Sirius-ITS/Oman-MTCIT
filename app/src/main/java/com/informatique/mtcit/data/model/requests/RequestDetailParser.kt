package com.informatique.mtcit.data.model.requests

import kotlinx.serialization.json.*
import java.util.Locale

/**
 * Utility to parse dynamic JSON request detail responses
 * Automatically traverses nested objects and arrays
 */
object RequestDetailParser {

    /**
     * Parse RequestDetailResponse into UI-friendly model
     */
    fun parseToUiModel(response: RequestDetailResponse): RequestDetailUiModel {
        val dataObject = response.data.jsonObject

        // Extract core fields
        val requestId = dataObject["id"]?.jsonPrimitive?.intOrNull ?: 0
        val requestSerial = dataObject["requestSerial"]?.jsonPrimitive?.intOrNull ?: 0
        val requestYear = dataObject["requestYear"]?.jsonPrimitive?.intOrNull ?: 0
        val message = dataObject["message"]?.jsonPrimitive?.contentOrNull
        val messageDetails = dataObject["messageDetails"]?.jsonPrimitive?.contentOrNull

        // Extract request type info
        val requestType = dataObject["requestType"]?.jsonObject?.let { rt ->
            RequestTypeInfo(
                id = rt["id"]?.jsonPrimitive?.intOrNull ?: 0,
                name = getLocalizedValue(rt, "name"),
                nameAr = rt["nameAr"]?.jsonPrimitive?.contentOrNull,
                nameEn = rt["nameEn"]?.jsonPrimitive?.contentOrNull
            )
        } ?: RequestTypeInfo(0, "Unknown", null, null)

        // Extract status info
        val status = dataObject["status"]?.jsonObject?.let { st ->
            RequestStatusInfo(
                id = st["id"]?.jsonPrimitive?.intOrNull ?: 0,
                name = getLocalizedValue(st, "name"),
                nameAr = st["nameAr"]?.jsonPrimitive?.contentOrNull,
                nameEn = st["nameEn"]?.jsonPrimitive?.contentOrNull
            )
        } ?: RequestStatusInfo(0, "Unknown", null, null)

        // ✅ Use ShipDataExtractor to get structured ship data sections
        val sections = ShipDataExtractor.extractShipDataSections(response.data)

        // ✅ Extract isPaid field (comes as string "0" or "1" from API)
        val isPaid = dataObject["isPaid"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0

        return RequestDetailUiModel(
            requestId = requestId,
            requestSerial = "$requestSerial/$requestYear",
            requestYear = requestYear,
            requestType = requestType,
            status = status,
            message = message,
            messageDetails = messageDetails,
            sections = sections,
            isPaid = isPaid
        )
    }

    /**
     * Get localized value based on current locale
     */
    private fun getLocalizedValue(jsonObject: JsonObject, fallbackKey: String): String {
        val isArabic = Locale.getDefault().language == "ar"

        return when {
            isArabic -> jsonObject["nameAr"]?.jsonPrimitive?.contentOrNull
            else -> jsonObject["nameEn"]?.jsonPrimitive?.contentOrNull
        } ?: jsonObject[fallbackKey]?.jsonPrimitive?.contentOrNull ?: "Unknown"
    }

    /**
     * Format field names for display (convert camelCase to readable text)
     */
    private fun formatFieldName(fieldName: String): String {
        // Convert camelCase to words
        val words = fieldName.replace(Regex("([a-z])([A-Z])"), "$1 $2")

        // Capitalize first letter
        return words.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault())
            else it.toString()
        }
    }
}
